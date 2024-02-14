/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.supervisor.thread;

import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.concurrent.AbstractHeartbeatThread;
import cn.ponfee.disjob.common.lock.DoInLocked;
import cn.ponfee.disjob.core.enums.ExecuteState;
import cn.ponfee.disjob.core.enums.RunState;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.supervisor.component.DistributedJobManager;
import cn.ponfee.disjob.supervisor.component.DistributedJobQuerier;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Date;
import java.util.List;

import static cn.ponfee.disjob.core.base.JobConstants.PROCESS_BATCH_SIZE;

/**
 * Scan running a long time, but still is running state sched_instance record.
 *
 * @author Ponfee
 */
public class RunningInstanceScanner extends AbstractHeartbeatThread {

    private final DoInLocked doInLocked;
    private final DistributedJobManager jobManager;
    private final DistributedJobQuerier jobQuerier;
    private final long beforeMilliseconds;

    public RunningInstanceScanner(long heartbeatPeriodMilliseconds,
                                  DoInLocked doInLocked,
                                  DistributedJobManager jobManager,
                                  DistributedJobQuerier jobQuerier) {
        super(heartbeatPeriodMilliseconds);
        SingletonClassConstraint.constrain(this);

        this.doInLocked = doInLocked;
        this.jobManager = jobManager;
        this.jobQuerier = jobQuerier;
        // heartbeat period duration: 30s * 8 = 240s
        this.beforeMilliseconds = (heartbeatPeriodMs << 3);
    }

    @Override
    protected boolean heartbeat() {
        if (jobManager.hasNotDiscoveredWorkers()) {
            log.warn("Not discovered worker.");
            return true;
        }

        Boolean result = doInLocked.action(this::process);
        return result != null && result;
    }

    private boolean process() {
        Date now = new Date();
        Date expireTime = new Date(now.getTime() - beforeMilliseconds);
        List<SchedInstance> instances = jobQuerier.findExpireRunningInstance(expireTime, PROCESS_BATCH_SIZE);
        if (CollectionUtils.isEmpty(instances)) {
            return true;
        }

        for (SchedInstance instance : instances) {
            processEach(instance, now);
        }

        return instances.size() < PROCESS_BATCH_SIZE;
    }

    private void processEach(SchedInstance instance, Date now) {
        if (!jobManager.renewInstanceUpdateTime(instance, now)) {
            return;
        }

        List<SchedTask> tasks = jobQuerier.findBaseInstanceTasks(instance.getInstanceId());
        List<SchedTask> waitingTasks = Collects.filter(tasks, e -> ExecuteState.WAITING.equals(e.getExecuteState()));

        if (CollectionUtils.isNotEmpty(waitingTasks)) {
            // 1、has waiting state task

            // sieve the (un-dispatch) or (assigned worker dead) waiting tasks to do re-dispatch
            List<SchedTask> redispatchingTasks = Collects.filter(waitingTasks, e -> jobManager.isDeadWorker(e.getWorker()));
            if (CollectionUtils.isEmpty(redispatchingTasks)) {
                return;
            }
            SchedJob schedJob = jobQuerier.getJob(instance.getJobId());
            if (schedJob == null) {
                log.error("Scanned running state instance not found job: {}", instance.getJobId());
                return;
            }
            // check is whether not discovered worker
            if (jobManager.hasNotDiscoveredWorkers(schedJob.getGroup())) {
                log.error("Scanned running state instance not discovered worker: {}, {}", instance.getInstanceId(), schedJob.getGroup());
                return;
            }
            log.info("Scanned running state instance re-dispatch task: {}", instance.getInstanceId());
            jobManager.dispatch(schedJob, instance, redispatchingTasks);

        } else if (tasks.stream().allMatch(e -> ExecuteState.of(e.getExecuteState()).isTerminal())) {
            // 2、all task was terminated

            // double check instance run state
            SchedInstance reloadInstance = jobQuerier.getInstance(instance.getInstanceId());
            if (reloadInstance == null) {
                log.error("Scanned running state instance not exists: {}", instance.getInstanceId());
                return;
            }
            if (RunState.of(reloadInstance.getRunState()).isTerminal()) {
                return;
            }
            log.info("Scanned running state instance task all terminated: {}", instance.getInstanceId());
            jobManager.purgeInstance(instance);

        } else {
            // 3、has executing state task

            // check has alive executing state task
            if (jobManager.hasAliveExecuting(tasks)) {
                return;
            }
            log.info("Scanned running state instance was dead: {}", instance.getInstanceId());
            jobManager.purgeInstance(instance);

        }
    }

}

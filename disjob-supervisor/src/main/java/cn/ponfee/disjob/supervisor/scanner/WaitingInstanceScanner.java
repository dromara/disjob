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

package cn.ponfee.disjob.supervisor.scanner;

import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.concurrent.AbstractHeartbeatThread;
import cn.ponfee.disjob.common.concurrent.PeriodExecutor;
import cn.ponfee.disjob.common.lock.LockTemplate;
import cn.ponfee.disjob.supervisor.component.JobManager;
import cn.ponfee.disjob.supervisor.component.JobQuerier;
import cn.ponfee.disjob.supervisor.component.WorkerClient;
import cn.ponfee.disjob.supervisor.configuration.SupervisorProperties;
import cn.ponfee.disjob.supervisor.model.SchedInstance;
import cn.ponfee.disjob.supervisor.model.SchedJob;
import cn.ponfee.disjob.supervisor.model.SchedTask;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Date;
import java.util.List;

/**
 * Scan expired trigger time, but still is waiting state sched_instance record.
 *
 * @author Ponfee
 */
public class WaitingInstanceScanner extends AbstractHeartbeatThread {

    private final int scanBatchSize;
    private final JobManager jobManager;
    private final JobQuerier jobQuerier;
    private final WorkerClient workerClient;
    private final LockTemplate lockTemplate;
    private final long beforeMilliseconds;
    private final PeriodExecutor logPrinter = new PeriodExecutor(30000, () -> log.warn("Not discovered any worker."));

    public WaitingInstanceScanner(SupervisorProperties conf,
                                  JobManager jobManager,
                                  JobQuerier jobQuerier,
                                  WorkerClient workerClient,
                                  LockTemplate lockTemplate) {
        super(conf.getScanWaitingInstancePeriodMs());
        SingletonClassConstraint.constrain(this);

        this.scanBatchSize = conf.getScanBatchSize();
        this.jobManager = jobManager;
        this.jobQuerier = jobQuerier;
        this.workerClient = workerClient;
        this.lockTemplate = lockTemplate;
        // heartbeat period duration: 10s * 12 = 120s
        this.beforeMilliseconds = (heartbeatPeriodMs * 12);
    }

    @Override
    protected boolean heartbeat() {
        if (!workerClient.hasAliveWorker()) {
            logPrinter.execute();
            return true;
        }

        Boolean result = lockTemplate.execute(this::process);
        return result != null && result;
    }

    // -------------------------------------------------------------process expire waiting sched instance

    private boolean process() {
        Date expireTime = new Date(System.currentTimeMillis() - beforeMilliseconds);
        List<SchedInstance> instances = jobQuerier.findExpireWaitingInstance(expireTime, scanBatchSize);
        if (CollectionUtils.isEmpty(instances)) {
            return true;
        }

        for (SchedInstance instance : instances) {
            processEach(instance);
        }
        return instances.size() < scanBatchSize;
    }

    private void processEach(SchedInstance instance) {
        if (!jobManager.updateInstanceNextScanTime(instance, new Date())) {
            return;
        }

        List<SchedTask> tasks = jobQuerier.findBaseInstanceTasks(instance.getInstanceId());
        List<SchedTask> waitingTasks = Collects.filter(tasks, SchedTask::isWaiting);
        if (CollectionUtils.isNotEmpty(waitingTasks)) {
            processHasWaitingTask(instance, waitingTasks);
        } else {
            processNoWaitingTask(instance, tasks);
        }
    }

    private void processHasWaitingTask(SchedInstance instance, List<SchedTask> waitingTasks) {
        // sieve the (un-dispatch) or (assigned worker dead) waiting tasks to do re-dispatch
        List<SchedTask> redispatchingTasks = Collects.filter(waitingTasks, workerClient::shouldRedispatch);
        if (CollectionUtils.isEmpty(redispatchingTasks)) {
            return;
        }
        SchedJob job = jobQuerier.getJob(instance.getJobId());
        if (job == null) {
            log.error("Scanned waiting state instance not found job: {}", instance.getJobId());
            return;
        }
        // check the group is whether none alive worker
        if (!workerClient.hasAliveWorker(job.getGroup())) {
            log.error("Scanned waiting state instance none alive worker: {}, {}", instance.getInstanceId(), job.getGroup());
            return;
        }
        jobManager.redispatch(job, instance, redispatchingTasks);
        log.info("Scanned waiting state instance re-dispatch task: {}", instance.getInstanceId());
    }

    private void processNoWaitingTask(SchedInstance instance, List<SchedTask> tasks) {
        if (tasks.stream().allMatch(SchedTask::isTerminal)) {
            // double check instance run state
            SchedInstance reloadInstance = jobQuerier.getInstance(instance.getInstanceId());
            if (reloadInstance == null) {
                log.error("Scanned waiting state instance not exists: {}", instance.getInstanceId());
                return;
            }
            if (reloadInstance.isTerminal()) {
                return;
            }
        }
        boolean purged = jobManager.purgeInstance(instance);
        log.info("Purge scanned waiting instance was dead: {}, {}", instance.getInstanceId(), purged);
    }

}

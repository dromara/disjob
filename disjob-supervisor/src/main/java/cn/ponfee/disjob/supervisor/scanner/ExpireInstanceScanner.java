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

import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.concurrent.AbstractHeartbeatThread;
import cn.ponfee.disjob.common.concurrent.PeriodExecutor;
import cn.ponfee.disjob.common.lock.LockTemplate;
import cn.ponfee.disjob.core.enums.RunState;
import cn.ponfee.disjob.supervisor.component.JobManager;
import cn.ponfee.disjob.supervisor.component.JobQuerier;
import cn.ponfee.disjob.supervisor.component.WorkerClient;
import cn.ponfee.disjob.supervisor.configuration.SupervisorProperties;
import cn.ponfee.disjob.supervisor.model.SchedInstance;
import cn.ponfee.disjob.supervisor.model.SchedJob;
import cn.ponfee.disjob.supervisor.model.SchedTask;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scan expire (waiting or running) instance record.
 *
 * @author Ponfee
 */
public class ExpireInstanceScanner extends AbstractHeartbeatThread {

    private static final Set<RunState> MUTEX = ConcurrentHashMap.newKeySet();
    private final RunState scanRunState;
    private final int scanBatchSize;
    private final JobManager jobManager;
    private final JobQuerier jobQuerier;
    private final WorkerClient workerClient;
    private final LockTemplate lockTemplate;
    private final long beforeMilliseconds;
    private final PeriodExecutor logPrinter = new PeriodExecutor(30000, () -> log.warn("Not discovered any worker."));

    public ExpireInstanceScanner(RunState scanRunState,
                                 SupervisorProperties conf,
                                 JobManager jobManager,
                                 JobQuerier jobQuerier,
                                 WorkerClient workerClient,
                                 LockTemplate lockTemplate) {
        super(obtainHeartbeatPeriodMs(scanRunState, conf));
        Assert.state(MUTEX.add(scanRunState), () -> "Expire Instance Scanner '" + scanRunState + "' already created.");

        this.scanRunState = scanRunState;
        this.scanBatchSize = conf.getScanBatchSize();
        this.jobManager = jobManager;
        this.jobQuerier = jobQuerier;
        this.workerClient = workerClient;
        this.lockTemplate = lockTemplate;
        // WAITING default: (15000 * 2) / 3 * 12 = 120000
        // RUNNING default: (30000 * 2) / 3 * 12 = 240000
        this.beforeMilliseconds = (heartbeatPeriodMs * 12);
    }

    @Override
    protected boolean heartbeat() {
        if (!workerClient.hasAliveWorker()) {
            logPrinter.execute();
            return true;
        }
        return Boolean.TRUE.equals(lockTemplate.execute(this::scan));
    }

    // --------------------------------------------------------------------------private methods

    private boolean scan() {
        Date expireTime = new Date(System.currentTimeMillis() - beforeMilliseconds);
        List<SchedInstance> instances = jobQuerier.findExpireInstance(scanRunState, expireTime, scanBatchSize);
        instances.forEach(this::processInstance);
        return instances.size() < scanBatchSize;
    }

    private void processInstance(SchedInstance instance) {
        if (jobManager.updateInstanceNextScanTime(instance, new Date())) {
            List<SchedTask> allTasks = jobQuerier.findBaseInstanceTasks(instance.getInstanceId());
            List<SchedTask> waitingTasks = Collects.filter(allTasks, SchedTask::isWaiting);
            if (CollectionUtils.isEmpty(waitingTasks)) {
                purgeExpiredInstance(instance, allTasks);
            } else {
                redispatchWaitingTask(instance, waitingTasks);
            }
        }
    }

    private void purgeExpiredInstance(SchedInstance instance, List<SchedTask> allTasks) {
        if (allTasks.stream().allMatch(SchedTask::isTerminal)) {
            // double check instance run state
            SchedInstance reloadInstance = jobQuerier.getInstance(instance.getInstanceId());
            if (reloadInstance == null) {
                log.error("Scanned instance not exists: {}", instance.getInstanceId());
                return;
            }
            if (reloadInstance.isTerminal()) {
                return;
            }
        } else {
            // check has alive executing state task
            if (scanRunState == RunState.RUNNING && workerClient.hasAliveTask(allTasks)) {
                return;
            }
        }
        boolean purged = jobManager.purgeInstance(instance);
        log.info("Purge scanned instance: {}, {}", instance.getInstanceId(), purged);
    }

    private void redispatchWaitingTask(SchedInstance instance, List<SchedTask> waitingTasks) {
        // sieve the (un-dispatch) or (assigned worker dead) waiting tasks to redo dispatch
        List<SchedTask> redispatchingTasks = Collects.filter(waitingTasks, workerClient::isNeedRedispatch);
        if (CollectionUtils.isEmpty(redispatchingTasks)) {
            return;
        }
        SchedJob job = jobQuerier.getJob(instance.getJobId());
        if (job == null) {
            log.error("Job not found: {}", instance.getJobId());
            return;
        }
        // check the group is whether none alive worker
        if (!workerClient.hasAliveWorker(job.getGroup())) {
            log.error("Group none alive worker: {}, {}", instance.getInstanceId(), job.getGroup());
            return;
        }
        jobManager.redispatch(job, instance, redispatchingTasks);
        log.info("Redo dispatch task: {}", instance.getInstanceId());
    }

    private static long obtainHeartbeatPeriodMs(RunState scanRunState, SupervisorProperties conf) {
        if (scanRunState == RunState.RUNNING) {
            return conf.getScanRunningInstancePeriodMs();
        } else if (scanRunState == RunState.WAITING) {
            return conf.getScanWaitingInstancePeriodMs();
        } else {
            throw new IllegalArgumentException("Unsupported expire instance scan run state: " + scanRunState);
        }
    }

}

/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.thread;

import cn.ponfee.disjob.common.lock.DoInLocked;
import cn.ponfee.disjob.common.util.Collects;
import cn.ponfee.disjob.core.base.AbstractHeartbeatThread;
import cn.ponfee.disjob.core.enums.ExecuteState;
import cn.ponfee.disjob.core.enums.RunState;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.supervisor.manager.DistributedJobManager;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Date;
import java.util.List;

import static cn.ponfee.disjob.core.base.JobConstants.PROCESS_BATCH_SIZE;

/**
 * Scan exceed the trigger time, but still is waiting state sched_instance record.
 *
 * @author Ponfee
 */
public class WaitingInstanceScanner extends AbstractHeartbeatThread {

    private final DoInLocked doInLocked;
    private final DistributedJobManager jobManager;
    private final long beforeMilliseconds;

    public WaitingInstanceScanner(long heartbeatPeriodMilliseconds,
                                  DoInLocked doInLocked,
                                  DistributedJobManager jobManager) {
        super(heartbeatPeriodMilliseconds);
        this.doInLocked = doInLocked;
        this.jobManager = jobManager;
        this.beforeMilliseconds = (heartbeatPeriodMs << 3); // 15s * 8 = 120s
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

    // -------------------------------------------------------------process expire waiting sched instance

    private boolean process() {
        Date now = new Date(), expireTime = new Date(now.getTime() - beforeMilliseconds);
        List<SchedInstance> instances = jobManager.findExpireWaitingInstance(expireTime, PROCESS_BATCH_SIZE);
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

        List<SchedTask> tasks = jobManager.findMediumInstanceTask(instance.getInstanceId());
        List<SchedTask> waitingTasks = Collects.filter(tasks, e -> ExecuteState.WAITING.equals(e.getExecuteState()));

        if (CollectionUtils.isNotEmpty(waitingTasks)) {
            // 1、has waiting state task

            // sieve the (un-dispatch) or (assigned worker dead) waiting tasks to do re-dispatch
            List<SchedTask> redispatchingTasks = Collects.filter(waitingTasks, e -> jobManager.isDeadWorker(e.getWorker()));
            if (CollectionUtils.isEmpty(redispatchingTasks)) {
                return;
            }
            SchedJob schedJob = jobManager.getJob(instance.getJobId());
            if (schedJob == null) {
                log.error("Scanned waiting state instance not found job: {}", instance.getJobId());
                return;
            }
            // check is whether not discovered worker
            if (jobManager.hasNotDiscoveredWorkers(schedJob.getJobGroup())) {
                log.error("Scanned waiting state instance not discovered worker: {} | {}", instance.getInstanceId(), schedJob.getJobGroup());
                return;
            }
            log.info("Scanned waiting state instance re-dispatch task: {}", instance.getInstanceId());
            jobManager.dispatch(schedJob, instance, redispatchingTasks);

        } else {
            // 2、waiting state instance unsupported other state task

            if (tasks.stream().allMatch(e -> ExecuteState.of(e.getExecuteState()).isTerminal())) {
                // double check instance run state
                SchedInstance reloadInstance = jobManager.getInstance(instance.getInstanceId());
                if (reloadInstance == null) {
                    log.error("Scanned waiting state instance not exists: {}", instance.getInstanceId());
                    return;
                }
                if (RunState.of(reloadInstance.getRunState()).isTerminal()) {
                    return;
                }
            }
            log.info("Scanned waiting state instance was dead: {}", instance.getInstanceId());
            jobManager.purgeInstance(instance);

        }
    }

}

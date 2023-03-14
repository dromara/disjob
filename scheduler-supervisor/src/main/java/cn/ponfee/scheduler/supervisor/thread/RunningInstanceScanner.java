/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.thread;

import cn.ponfee.scheduler.common.lock.DoInLocked;
import cn.ponfee.scheduler.common.util.Collects;
import cn.ponfee.scheduler.core.base.AbstractHeartbeatThread;
import cn.ponfee.scheduler.core.enums.ExecuteState;
import cn.ponfee.scheduler.core.model.SchedInstance;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.supervisor.manager.SchedulerJobManager;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Date;
import java.util.List;

import static cn.ponfee.scheduler.core.base.JobConstants.PROCESS_BATCH_SIZE;

/**
 * Scan running a long time, but still is running state sched_instance record.
 *
 * @author Ponfee
 */
public class RunningInstanceScanner extends AbstractHeartbeatThread {

    private final DoInLocked doInLocked;
    private final SchedulerJobManager schedulerJobManager;
    private final long beforeMilliseconds;

    public RunningInstanceScanner(long heartbeatPeriodMilliseconds,
                                  DoInLocked doInLocked,
                                  SchedulerJobManager schedulerJobManager) {
        super(heartbeatPeriodMilliseconds);
        this.doInLocked = doInLocked;
        this.schedulerJobManager = schedulerJobManager;
        this.beforeMilliseconds = (heartbeatPeriodMs << 3); // 30s * 8 = 240s
    }

    @Override
    protected boolean heartbeat() {
        if (schedulerJobManager.hasNotDiscoveredWorkers()) {
            log.warn("Not found available worker.");
            return true;
        }

        Boolean result = doInLocked.apply(this::process);
        return result == null || result;
    }

    private boolean process() {
        Date now = new Date(), expireTime = new Date(now.getTime() - beforeMilliseconds);
        List<SchedInstance> instances = schedulerJobManager.findExpireRunning(expireTime, PROCESS_BATCH_SIZE);
        if (CollectionUtils.isEmpty(instances)) {
            return true;
        }

        for (SchedInstance instance : instances) {
            processEach(instance, now);
        }

        return instances.size() < PROCESS_BATCH_SIZE;
    }

    private void processEach(SchedInstance instance, Date now) {
        List<SchedTask> tasks = schedulerJobManager.findMediumTaskByInstanceId(instance.getInstanceId());

        // sieve the waiting tasks
        List<SchedTask> waitingTasks = Collects.filter(tasks, e -> ExecuteState.WAITING.equals(e.getExecuteState()));
        if (CollectionUtils.isNotEmpty(waitingTasks)) {
            if (!schedulerJobManager.renewUpdateTime(instance, now)) {
                return;
            }
            // sieve the un-dispatch waiting tasks to do re-dispatch
            List<SchedTask> dispatchingTasks = schedulerJobManager.filterDispatchingTask(waitingTasks);
            if (CollectionUtils.isEmpty(dispatchingTasks)) {
                return;
            }
            SchedJob schedJob = schedulerJobManager.getJob(instance.getJobId());
            if (schedJob == null) {
                log.error("Running instance scanner sched job not found: {}", instance.getJobId());
                schedulerJobManager.updateInstanceState(ExecuteState.DATA_INVALID, tasks, instance);
                return;
            }
            log.info("Running scanner redispatch sched instance: {}", instance);
            schedulerJobManager.dispatch(schedJob, instance, dispatchingTasks);
            return;
        }

        // check has executing task
        List<SchedTask> executingTasks = Collects.filter(tasks, e -> ExecuteState.EXECUTING.equals(e.getExecuteState()));
        if (CollectionUtils.isNotEmpty(executingTasks) && schedulerJobManager.hasAliveExecuting(executingTasks)) {
            schedulerJobManager.renewUpdateTime(instance, now);
            return;
        }

        // all workers are dead
        if (schedulerJobManager.renewUpdateTime(instance, now)) {
            log.info("Scanned the running state instance death, because all task was terminal: {}", instance.getInstanceId());
            schedulerJobManager.deathInstance(instance.getInstanceId());
        }
    }

}

/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.thread;

import cn.ponfee.scheduler.common.lock.DoInLocked;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.base.AbstractHeartbeatThread;
import cn.ponfee.scheduler.core.enums.ExecuteState;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.core.model.SchedTrack;
import cn.ponfee.scheduler.supervisor.manager.SchedulerJobManager;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static cn.ponfee.scheduler.core.base.JobConstants.PROCESS_BATCH_SIZE;

/**
 * Scan running a long time, but still is running state sched_track record.
 *
 * @author Ponfee
 */
public class ScanRunningTrackThread extends AbstractHeartbeatThread {

    private final DoInLocked doInLocked;
    private final SchedulerJobManager schedulerJobManager;
    private final long beforeMilliseconds;

    public ScanRunningTrackThread(long heartbeatPeriodMs0,
                                  DoInLocked doInLocked,
                                  SchedulerJobManager schedulerJobManager) {
        super(heartbeatPeriodMs0);
        this.doInLocked = doInLocked;
        this.schedulerJobManager = schedulerJobManager;
        this.beforeMilliseconds = (heartbeatPeriodMs << 2); // 30s * 4 = 120s
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
        List<SchedTrack> tracks = schedulerJobManager.findExpireRunning(expireTime, PROCESS_BATCH_SIZE);
        if (CollectionUtils.isEmpty(tracks)) {
            return true;
        }

        for (SchedTrack track : tracks) {
            processEach(track, now);
        }

        return tracks.size() < PROCESS_BATCH_SIZE;
    }

    private void processEach(SchedTrack track, Date now) {
        List<SchedTask> tasks = schedulerJobManager.findMediumTaskByTrackId(track.getTrackId());

        // sieve the waiting tasks
        List<SchedTask> waitingTasks = tasks.stream()
            .filter(e -> ExecuteState.WAITING.equals(e.getExecuteState()))
            .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(waitingTasks)) {
            boolean isUpdateSuccess = schedulerJobManager.renewUpdateTime(track, now);
            if (isUpdateSuccess) {
                // sieve the un-dispatch waiting tasks to do re-dispatch
                List<SchedTask> dispatchingTasks = schedulerJobManager.filterDispatchingTask(waitingTasks);
                if (CollectionUtils.isNotEmpty(dispatchingTasks)) {
                    log.info("Redispatch sched track: {} | {}", track, Jsons.toJson(dispatchingTasks));
                    SchedJob schedJob = schedulerJobManager.getJob(track.getJobId());
                    schedulerJobManager.dispatch(schedJob, track, dispatchingTasks);
                }
            }
            return;
        }

        // check has executing task
        if (schedulerJobManager.hasAliveExecuting(tasks)) {
            schedulerJobManager.renewUpdateTime(track, now);
            return;
        }

        // all workers are dead
        log.info("Scan track, all worker dead, terminate the sched track: {}", track.getTrackId());
        schedulerJobManager.terminate(track.getTrackId());
    }

}

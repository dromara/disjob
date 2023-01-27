/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.thread;

import cn.ponfee.scheduler.common.date.Dates;
import cn.ponfee.scheduler.common.lock.DoInLocked;
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
 * Scan exceed the trigger time, but still is waiting state sched_track record.
 *
 * @author Ponfee
 */
public class WaitingTrackScanner extends AbstractHeartbeatThread {

    private final DoInLocked doInLocked;
    private final SchedulerJobManager schedulerJobManager;
    private final long beforeMilliseconds;

    public WaitingTrackScanner(long heartbeatPeriodMs0,
                               DoInLocked doInLocked,
                               SchedulerJobManager schedulerJobManager) {
        super(heartbeatPeriodMs0);
        this.doInLocked = doInLocked;
        this.schedulerJobManager = schedulerJobManager;
        this.beforeMilliseconds = (heartbeatPeriodMs << 3); // 5s * 8 = 40s
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

    // -------------------------------------------------------------process expire waiting sched track

    private boolean process() {
        Date now = new Date(), expireTime = new Date(now.getTime() - beforeMilliseconds);
        List<SchedTrack> tracks = schedulerJobManager.findExpireWaiting(expireTime, PROCESS_BATCH_SIZE);
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

        if (tasks.stream().allMatch(t -> ExecuteState.of(t.getExecuteState()).isTerminal())) {
            // if all the tasks are terminal, then terminate sched track record
            if (schedulerJobManager.renewUpdateTime(track, now)) {
                log.info("All task terminal, terminate the sched track: {}", track.getTrackId());
                schedulerJobManager.terminate(track.getTrackId());
            }
            return;
        }

        // sieve the waiting tasks
        List<SchedTask> waitingTasks = tasks.stream()
            .filter(e -> ExecuteState.WAITING.equals(e.getExecuteState()))
            .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(waitingTasks)) {
            // read conflict
            log.info("Not has waiting tasks: {}", track);
            return;
        }

        // filter dispatching task
        List<SchedTask> dispatchingTasks = schedulerJobManager.filterDispatchingTask(waitingTasks);
        if (CollectionUtils.isEmpty(dispatchingTasks)) {
            // none un-dispatched task
            schedulerJobManager.renewUpdateTime(track, now);
            return;
        }

        SchedJob job = schedulerJobManager.getJob(track.getJobId());
        if (job == null) {
            log.error("Job not exists: {}, {}", track, tasks);
            schedulerJobManager.updateState(ExecuteState.DATA_INCONSISTENT, tasks, track);
            return;
        }

        // check not found worker
        if (schedulerJobManager.hasNotDiscoveredWorkers(job.getJobGroup())) {
            schedulerJobManager.renewUpdateTime(track, now);
            log.warn("Scan track not found available group '{}' workers.", job.getJobGroup());
            return;
        }

        if (schedulerJobManager.renewUpdateTime(track, now)) {
            if (log.isInfoEnabled()) {
                log.info("Redispatch sched track: {} | {}", track, Dates.format(now));
            }
            schedulerJobManager.dispatch(job, track, dispatchingTasks);
        }
    }

}

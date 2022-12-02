package cn.ponfee.scheduler.supervisor;

import cn.ponfee.scheduler.common.date.Dates;
import cn.ponfee.scheduler.common.lock.DoInLocked;
import cn.ponfee.scheduler.core.base.AbstractHeartbeatThread;
import cn.ponfee.scheduler.core.enums.ExecuteState;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.core.model.SchedTrack;
import cn.ponfee.scheduler.supervisor.manager.JobManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scan exceed the trigger time, but still waiting state sched_track data.
 *
 * @author Ponfee
 */
public class ScanTrackHeartbeatThread extends AbstractHeartbeatThread {

    private static final int QUERY_BATCH_SIZE = 200;
    private static final long EXPIRE_WAITING_MILLISECONDS = 60 * 1000;
    private static final long EXPIRE_RUNNING_MILLISECONDS = 120 * 1000;

    private final DoInLocked doInLocked;
    private final JobManager jobManager;

    private long nextScanExpireRunningTimeMillis = 0;

    public ScanTrackHeartbeatThread(int heartbeatIntervalSeconds,
                                    DoInLocked doInLocked,
                                    JobManager jobManager) {
        super(heartbeatIntervalSeconds);
        this.doInLocked = doInLocked;
        this.jobManager = jobManager;
    }

    @Override
    protected boolean heartbeat() {
        if (jobManager.hasNotFoundWorkers()) {
            return false;
        }
        Boolean result = doInLocked.apply(() -> {
            Date now = new Date();
            return processExpireWaiting(now)
                || processExpireRunning(now);
        });

        return result != null && result;
    }

    // -------------------------------------------------------------process expire waiting sched track
    private boolean processExpireWaiting(Date now) {
        long expireTime = now.getTime() - EXPIRE_WAITING_MILLISECONDS;
        List<SchedTrack> tracks = jobManager.findExpireWaiting(expireTime, new Date(expireTime), QUERY_BATCH_SIZE);
        if (tracks == null || tracks.isEmpty()) {
            return false;
        }

        for (SchedTrack track : tracks) {
            processExpireWaiting(track, now);
        }
        return true;
    }

    private void processExpireWaiting(SchedTrack track, Date now) {
        List<SchedTask> tasks = jobManager.findTasks(track.getTrackId());

        if (tasks.stream().allMatch(t -> ExecuteState.of(t.getExecuteState()).isTerminal())) {
            // if all the tasks are terminal, then terminate sched track record
            if (jobManager.renewUpdateTime(track, now)) {
                log.info("All task terminal, terminate the sched track: {}", track.getTrackId());
                jobManager.terminate(track.getTrackId());
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

        SchedJob job = jobManager.getJob(track.getJobId());
        if (job == null) {
            log.error("Job not exists: {}, {}", track, tasks);
            jobManager.updateState(ExecuteState.DATA_INCONSISTENT, tasks, track);
            return;
        }

        // check not found worker
        if (jobManager.hasNotFoundWorkers(job.getJobGroup())) {
            jobManager.renewUpdateTime(track, now);
            log.warn("Scan track not found available group '{}' workers.", job.getJobGroup());
            return;
        }

        if (jobManager.renewUpdateTime(track, now)) {
            log.info("Redispatch sched track: {} - {}", track, Dates.format(now));
            jobManager.dispatch(job, track, waitingTasks);
        }
    }

    // -------------------------------------------------------------process expire running sched track
    private boolean processExpireRunning(Date now) {
        if (now.getTime() < nextScanExpireRunningTimeMillis) {
            return false;
        }

        long expireTime = now.getTime() - EXPIRE_RUNNING_MILLISECONDS;
        List<SchedTrack> tracks = jobManager.findExpireRunning(expireTime, new Date(expireTime), QUERY_BATCH_SIZE);
        boolean noResult = CollectionUtils.isEmpty(tracks);
        if (noResult || tracks.size() < QUERY_BATCH_SIZE) {
            this.nextScanExpireRunningTimeMillis = now.getTime() + EXPIRE_RUNNING_MILLISECONDS;
        }
        if (noResult) {
            return false;
        }

        for (SchedTrack track : tracks) {
            processExpireRunning(track, now);
        }
        return true;
    }

    private void processExpireRunning(SchedTrack track, Date now) {
        List<SchedTask> tasks = jobManager.findByTrackId(track.getTrackId());

        // sieve the waiting tasks
        List<SchedTask> waitingTasks = tasks.stream()
            .filter(e -> ExecuteState.WAITING.equals(e.getExecuteState()))
            .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(waitingTasks)) {
            if (jobManager.renewUpdateTime(track, now)) {
                log.info("Redispatch sched track: {} - {}", track, Dates.format(now));
                jobManager.dispatch(jobManager.getJob(track.getJobId()), track, waitingTasks);
            }
            return;
        }

        // check has executing task
        boolean hasAliveExecuting = tasks.stream()
            .filter(e -> ExecuteState.EXECUTING.equals(e.getExecuteState()))
            .map(SchedTask::getWorker)
            .filter(StringUtils::isNotBlank)
            .anyMatch(jobManager::isAliveWorker);
        if (hasAliveExecuting) {
            jobManager.renewUpdateTime(track, now);
            return;
        }

        // all workers are dead
        log.info("Scan track, all worker dead, terminate the sched track: {}", track.getTrackId());
        jobManager.terminate(track.getTrackId());
    }

}

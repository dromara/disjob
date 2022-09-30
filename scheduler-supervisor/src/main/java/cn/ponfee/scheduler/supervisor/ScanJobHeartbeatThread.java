package cn.ponfee.scheduler.supervisor;

import cn.ponfee.scheduler.common.base.IdGenerator;
import cn.ponfee.scheduler.common.base.exception.CheckedThrowing;
import cn.ponfee.scheduler.common.date.Dates;
import cn.ponfee.scheduler.common.lock.DoInLocked;
import cn.ponfee.scheduler.core.base.AbstractHeartbeatThread;
import cn.ponfee.scheduler.core.enums.*;
import cn.ponfee.scheduler.core.exception.JobException;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.core.model.SchedTrack;
import cn.ponfee.scheduler.supervisor.base.SupervisorConstants;
import cn.ponfee.scheduler.supervisor.manager.JobManager;
import cn.ponfee.scheduler.supervisor.util.JobUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The schedule job heartbeat thread, <br/>
 * find the sched_job which will be trigger, <br/>
 * split to one sched_track and many sched_task
 *
 * @author Ponfee
 */
public class ScanJobHeartbeatThread extends AbstractHeartbeatThread {

    private static final int QUERY_BATCH_SIZE = 200;

    private final DoInLocked doInLocked;
    private final JobManager jobManager;
    private final IdGenerator idGenerator;
    private final long afterSeconds;

    public ScanJobHeartbeatThread(int heartbeatIntervalSeconds,
                                  DoInLocked doInLocked,
                                  JobManager jobManager,
                                  IdGenerator idGenerator) {
        super(heartbeatIntervalSeconds);
        this.doInLocked = doInLocked;
        this.jobManager = jobManager;
        this.idGenerator = idGenerator;
        this.afterSeconds = interval() << 1;
    }

    @Override
    protected boolean heartbeat() {
        Boolean result = doInLocked.apply(() -> {
            Date now = new Date();
            long maxNextTriggerTime = now.getTime() + afterSeconds;
            List<SchedJob> jobs = jobManager.findBeTriggering(maxNextTriggerTime, QUERY_BATCH_SIZE);
            if (jobs == null || jobs.isEmpty()) {
                return false;
            }
            for (SchedJob job : jobs) {
                processJob(job, now, maxNextTriggerTime);
            }
            return true;
        });

        return result != null && result;
    }

    private void processJob(SchedJob job, Date now, long maxNextTriggerTime) {
        try {
            // 重新再计算一次nextTriggerTime
            job.setNextTriggerTime(recomputeNextTriggerTime(job, now));
            if (job.getNextTriggerTime() == null) {
                job.setRemark("Stop recompute reason: has not next trigger time");
                logger.info(job.getRemark() + ": " + job);
                jobManager.stopJob(job);
                return;
            } else if (job.getNextTriggerTime() > maxNextTriggerTime) {
                jobManager.updateNextTriggerTime(job);
                return;
            }

            // check has available workers
            if (!jobManager.hasWorkers(job.getJobGroup())) {
                updateNextScanTime(job, now, 15);
                logger.warn("Not found available group '{}' workers.", job.getJobGroup());
                return;
            }

            // check collision with last schedule
            if (checkBlockCollisionTrigger(job, now)) {
                return;
            }

            // 1、build sched track and sched task list
            Pair<SchedTrack, List<SchedTask>> pair = JobUtils.buildTrackAndTasks(job, now, idGenerator::generateId);
            SchedTrack track = pair.getLeft();
            List<SchedTask> tasks = pair.getRight();
            Assert.notEmpty(tasks, "Invalid split, Not has executable task: " + job);

            // 2、refresh next trigger time
            refreshNextTriggerTime(job, job.getNextTriggerTime(), now);

            // 3、update and save data
            boolean result = jobManager.updateAndSave(job, track, tasks);
            if (result) {
                // 4、dispatch job task
                jobManager.dispatch(job, track, tasks);
            }
        } catch (JobException | IllegalArgumentException e) {
            logger.error(e.getMessage() + ": " + job, e);
            job.setRemark("Stop reason: " + e.getMessage());
            job.setNextTriggerTime(null);
            jobManager.stopJob(job);
        } catch (Exception e) {
            logger.error("Process handle job occur error: " + job, e);
            if (e instanceof DuplicateKeyException) {
                if (jobManager.updateNextTriggerTime(job)) {
                    logger.info("Conflict trigger time, update the job next trigger time {}", job);
                }
            }
        }
    }

    /**
     * Recompute the job next trigger time.
     *
     * @param job the job
     * @param now the current date
     * @return accurate next trigger time milliseconds
     */
    private Long recomputeNextTriggerTime(SchedJob job, Date now) {
        if (now.getTime() <= (job.getNextTriggerTime() + afterSeconds)) {
            // 1、如果没有过期：保持原有的nextTriggerTime
            return job.getNextTriggerTime();
        } else {
            // 2、其它情况：基于原来的lastTriggerTime重新再计算一次nextTriggerTime
            return JobUtils.computeNextTriggerTime(job, now);
        }
    }

    /**
     * Check is whether block if the next trigger collision
     *
     * @param job the sched job
     * @param now the now date time
     * @return {@code true} will block the next trigger
     */
    private boolean checkBlockCollisionTrigger(SchedJob job, Date now) throws JobException {
        CollisionStrategy collisionStrategy = CollisionStrategy.of(job.getCollisionStrategy());
        Long lastTriggerTime;
        if (CollisionStrategy.CONCURRENT == collisionStrategy || (lastTriggerTime = job.getLastTriggerTime()) == null) {
            return false;
        }

        SchedTrack lastTrack = jobManager.getByTriggerTime(job.getJobId(), lastTriggerTime, RunType.SCHEDULE.value());
        if (lastTrack == null) {
            return false;
        }

        long trackId = lastTrack.getTrackId();
        RunState runState = RunState.of(lastTrack.getRunState());
        switch (runState) {
            case FINISHED:
                return false;
            case WAITING:
            case PAUSED:
                return checkBlockCollisionTrigger(job, Collections.singletonList(trackId), collisionStrategy, now);
            case RUNNING:
                if (jobManager.hasAliveExecuting(trackId)) {
                    return checkBlockCollisionTrigger(job, Collections.singletonList(trackId), collisionStrategy, now);
                } else {
                    // all workers are dead
                    logger.info("Collision, all worker dead, terminate the sched track: {}", trackId);
                    jobManager.cancelTrack(trackId, Operations.COLLISION_CANCEL);
                    return false;
                }
            case CANCELED:
                List<SchedTrack> list = jobManager.findUnterminatedRetry(trackId);
                if (CollectionUtils.isEmpty(list)) {
                    return false;
                } else {
                    List<Long> trackIds = list.stream().map(SchedTrack::getTrackId).collect(Collectors.toList());
                    return checkBlockCollisionTrigger(job, trackIds, collisionStrategy, now);
                }
            default:
                throw new UnsupportedOperationException("Unsupported run state: " + runState.name());
        }
    }

    private boolean checkBlockCollisionTrigger(SchedJob job, List<Long> trackIds, CollisionStrategy collisionStrategy, Date now) {
        switch (collisionStrategy) {
            case DISCARD:
                // 丢弃执行：基于当前时间来更新下一次的执行时间
                Integer misfireStrategy = job.getMisfireStrategy();
                try {
                    job.setMisfireStrategy(MisfireStrategy.DISCARD.value());
                    job.setNextTriggerTime(JobUtils.computeNextTriggerTime(job, now));
                } finally {
                    // restore
                    job.setMisfireStrategy(misfireStrategy);
                }
                if (job.getNextTriggerTime() == null) {
                    // It has not next triggered time, then stop the job
                    job.setRemark("Stop collision reason: has not next trigger time.");
                    job.setJobState(JobState.STOPPED.value());
                }
                jobManager.updateNextTriggerTime(job);
                return true;
            case SERIAL:
                // 串行执行：更新下一次的扫描时间
                updateNextScanTime(job, now, SupervisorConstants.SCAN_TIME_INTERVAL_SECONDS);
                return true;
            case OVERRIDE:
                // 覆盖执行：先取消上一次的执行
                trackIds.forEach(trackId -> CheckedThrowing.supplier(() -> jobManager.cancelTrack(trackId, Operations.COLLISION_CANCEL)));
                return false;
            default:
                throw new UnsupportedOperationException("Unsupported collision strategy: " + collisionStrategy.name());
        }
    }

    private void updateNextScanTime(SchedJob job, Date now, int delayedSeconds) {
        Date nextScanTime = Dates.plusSeconds(now, delayedSeconds);
        jobManager.updateNextScanTime(job.getJobId(), nextScanTime, job.getVersion());
    }

    /**
     * Refresh the job next trigger time.
     *
     * @param job             the job
     * @param lastTriggerTime the lastTriggerTime
     * @param now             the current date
     */
    private static void refreshNextTriggerTime(SchedJob job, Long lastTriggerTime, Date now) {
        job.setLastTriggerTime(lastTriggerTime);
        job.setNextTriggerTime(JobUtils.computeNextTriggerTime(job, now));
        if (job.getNextTriggerTime() == null) {
            // It has not next triggered time, then stop the job
            job.setRemark("Stop refresh reason: has not next trigger time");
            job.setJobState(JobState.STOPPED.value());
        }
    }

}

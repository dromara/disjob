/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.thread;

import cn.ponfee.disjob.common.concurrent.NamedThreadFactory;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingSupplier;
import cn.ponfee.disjob.common.lock.DoInLocked;
import cn.ponfee.disjob.core.base.AbstractHeartbeatThread;
import cn.ponfee.disjob.core.enums.*;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.supervisor.instance.TriggerInstanceCreator;
import cn.ponfee.disjob.supervisor.manager.DistributedJobManager;
import cn.ponfee.disjob.supervisor.util.TriggerTimeUtils;
import com.google.common.math.IntMath;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.stream.Collectors;

import static cn.ponfee.disjob.core.base.JobConstants.PROCESS_BATCH_SIZE;

/**
 * The schedule job heartbeat thread, <br/>
 * find the sched_job which will be trigger, <br/>
 * split to one sched_instance and many sched_task
 *
 * @author Ponfee
 */
public class TriggeringJobScanner extends AbstractHeartbeatThread {

    private static final int SCAN_COLLISION_INTERVAL_SECONDS = 60;
    private static final int REMARK_MAX_LENGTH = 255;
    private static final int FAILED_SCAN_COUNT_THRESHOLD = 5;

    private final DoInLocked doInLocked;
    private final DistributedJobManager jobManager;
    private final long afterMilliseconds;
    private final ExecutorService processJobExecutor;

    public TriggeringJobScanner(long heartbeatPeriodMilliseconds,
                                int processJobMaximumPoolSize,
                                DoInLocked doInLocked,
                                DistributedJobManager jobManager) {
        super(heartbeatPeriodMilliseconds);
        this.doInLocked = doInLocked;
        this.jobManager = jobManager;
        this.afterMilliseconds = (heartbeatPeriodMs * 3); // 3s * 3 = 9s
        this.processJobExecutor = ThreadPoolExecutors.builder()
            .corePoolSize(1)
            .maximumPoolSize(Math.max(1, processJobMaximumPoolSize))
            .workQueue(new SynchronousQueue<>())
            .keepAliveTimeSeconds(300)
            .rejectedHandler(ThreadPoolExecutors.CALLER_RUNS)
            .threadFactory(NamedThreadFactory.builder().prefix("triggering_job_scanner").priority(Thread.MAX_PRIORITY).build())
            .prestartCoreThreadType(ThreadPoolExecutors.PrestartCoreThreadType.ONE)
            .build();
    }

    @Override
    protected boolean heartbeat() {
        if (jobManager.hasNotDiscoveredWorkers()) {
            log.warn("Not discovered worker.");
            return true;
        }
        Boolean result = doInLocked.action(() -> {
            Date now = new Date();
            long maxNextTriggerTime = now.getTime() + afterMilliseconds;
            List<SchedJob> jobs = jobManager.findBeTriggeringJob(maxNextTriggerTime, PROCESS_BATCH_SIZE);
            if (CollectionUtils.isEmpty(jobs)) {
                return true;
            }

            jobs.stream()
                .map(job -> CompletableFuture.runAsync(() -> processJob(job, now, maxNextTriggerTime), processJobExecutor))
                .collect(Collectors.toList())
                .forEach(CompletableFuture::join);

            return jobs.size() < PROCESS_BATCH_SIZE;
        });

        return result != null && result;
    }

    @Override
    public void close() {
        super.close();
        ThrowingSupplier.caught(() -> ThreadPoolExecutors.shutdown(processJobExecutor, 3));
    }

    private void processJob(SchedJob job, Date now, long maxNextTriggerTime) {
        try {
            // 重新再计算一次nextTriggerTime
            job.setNextTriggerTime(recomputeNextTriggerTime(job, now));
            if (job.getNextTriggerTime() == null) {
                String reason = "Recompute has not next trigger time";
                job.setRemark(reason);
                log.info("{} | {}", reason, job);
                jobManager.stopJob(job);
                return;
            } else if (job.getNextTriggerTime() > maxNextTriggerTime) {
                jobManager.updateJobNextTriggerTime(job);
                return;
            }

            // check has available workers
            if (jobManager.hasNotDiscoveredWorkers(job.getJobGroup())) {
                updateNextScanTime(job, now, 15);
                log.warn("Scan job not discovered worker: {} | {}", job.getJobId(), job.getJobGroup());
                return;
            }

            // check collision with last schedule
            if (checkBlockCollisionTrigger(job, now)) {
                return;
            }

            long triggerTime = job.getNextTriggerTime();
            refreshNextTriggerTime(job, triggerTime, now);

            TriggerInstanceCreator<?> creator = TriggerInstanceCreator.of(job.getJobType(), jobManager);
            creator.createThenSaveAndDispatch(job, RunType.SCHEDULE, triggerTime);

        } catch (DuplicateKeyException e) {
            if (jobManager.updateJobNextTriggerTime(job)) {
                log.info("Conflict trigger time: {} | {}", job, e.getMessage());
            } else {
                log.error("Conflict trigger time: {} | {}", job, e.getMessage());
            }
        } catch (IllegalArgumentException e) {
            log.error("Scan trigger job failed: " + job, e);
            job.setRemark(StringUtils.truncate("Scan process failed: " + e.getMessage(), REMARK_MAX_LENGTH));
            job.setNextTriggerTime(null);
            jobManager.stopJob(job);
        } catch (Throwable t) {
            log.error("Scan trigger job error: " + job, t);
            if (job.getFailedScanCount() >= FAILED_SCAN_COUNT_THRESHOLD) {
                job.setRemark(StringUtils.truncate("Scan over failed: " + t.getMessage(), REMARK_MAX_LENGTH));
                job.setNextTriggerTime(null);
                jobManager.stopJob(job);
            } else {
                int failedScanCount = job.incrementAndGetFailedScanCount();
                updateNextScanTime(job, now, IntMath.pow(failedScanCount, 2) * 5);
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
        if (now.getTime() <= (job.getNextTriggerTime() + afterMilliseconds)) {
            // 1、如果没有过期：保持原有的nextTriggerTime
            return job.getNextTriggerTime();
        } else {
            // 2、其它情况：基于原来的lastTriggerTime重新再计算一次nextTriggerTime
            return TriggerTimeUtils.computeNextTriggerTime(job, now);
        }
    }

    /**
     * Check is whether block if the next trigger collision
     *
     * @param job the sched job
     * @param now the now date time
     * @return {@code true} will block the next trigger
     */
    private boolean checkBlockCollisionTrigger(SchedJob job, Date now) {
        CollisionStrategy collisionStrategy = CollisionStrategy.of(job.getCollisionStrategy());
        Long lastTriggerTime;
        if (CollisionStrategy.CONCURRENT == collisionStrategy || (lastTriggerTime = job.getLastTriggerTime()) == null) {
            return false;
        }

        SchedInstance lastInstance = jobManager.getInstance(job.getJobId(), lastTriggerTime, RunType.SCHEDULE.value());
        if (lastInstance == null) {
            return false;
        }

        long instanceId = lastInstance.getInstanceId();
        RunState runState = RunState.of(lastInstance.getRunState());
        switch (runState) {
            case FINISHED:
                return false;
            case WAITING:
            case PAUSED:
                return checkBlockCollisionTrigger(job, Collections.singletonList(lastInstance), collisionStrategy, now);
            case RUNNING:
                List<SchedTask> tasks = jobManager.findBaseInstanceTask(instanceId);
                if (jobManager.hasAliveExecuting(tasks)) {
                    return checkBlockCollisionTrigger(job, Collections.singletonList(lastInstance), collisionStrategy, now);
                } else {
                    // all workers are dead
                    log.info("Collision, all worker dead, terminate the sched instance: {}", instanceId);
                    jobManager.cancelInstance(instanceId, lastInstance.getWnstanceId(), Operations.COLLISION_CANCEL);
                    return false;
                }
            case CANCELED:
                List<SchedInstance> list = jobManager.findUnterminatedRetryInstance(instanceId);
                if (CollectionUtils.isEmpty(list)) {
                    return false;
                } else {
                    return checkBlockCollisionTrigger(job, list, collisionStrategy, now);
                }
            default:
                throw new UnsupportedOperationException("Unsupported run state: " + runState.name());
        }
    }

    private boolean checkBlockCollisionTrigger(SchedJob job, List<SchedInstance> instances, CollisionStrategy collisionStrategy, Date now) {
        switch (collisionStrategy) {
            case DISCARD:
                // 丢弃执行：基于当前时间来更新下一次的执行时间
                Integer misfireStrategy = job.getMisfireStrategy();
                try {
                    job.setMisfireStrategy(MisfireStrategy.DISCARD.value());
                    job.setNextTriggerTime(TriggerTimeUtils.computeNextTriggerTime(job, now));
                } finally {
                    // restore
                    job.setMisfireStrategy(misfireStrategy);
                }
                if (job.getNextTriggerTime() == null) {
                    // It has not next triggered time, then stop the job
                    job.setRemark("Disable collision reason: has not next trigger time.");
                    job.setJobState(JobState.DISABLE.value());
                }
                jobManager.updateJobNextTriggerTime(job);
                return true;
            case SERIAL:
                // 串行执行：更新下一次的扫描时间
                updateNextScanTime(job, now, SCAN_COLLISION_INTERVAL_SECONDS);
                return true;
            case OVERRIDE:
                // 覆盖执行：先取消上一次的执行
                instances.forEach(e -> jobManager.cancelInstance(e.getInstanceId(), e.getWnstanceId(), Operations.COLLISION_CANCEL));
                return false;
            default:
                throw new UnsupportedOperationException("Unsupported collision strategy: " + collisionStrategy.name());
        }
    }

    private void updateNextScanTime(SchedJob job, Date now, int delayedSeconds) {
        job.setNextScanTime(Dates.plusSeconds(now, delayedSeconds));
        jobManager.updateJobNextScanTime(job);
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
        job.setNextTriggerTime(TriggerTimeUtils.computeNextTriggerTime(job, now));
        if (job.getNextTriggerTime() == null) {
            // It has not next triggered time, then stop the job
            job.setRemark("Disable refresh reason: has not next trigger time");
            job.setJobState(JobState.DISABLE.value());
        }
    }

}

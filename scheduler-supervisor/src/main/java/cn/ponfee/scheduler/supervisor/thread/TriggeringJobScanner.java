/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.thread;

import cn.ponfee.scheduler.common.concurrent.NamedThreadFactory;
import cn.ponfee.scheduler.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.scheduler.common.date.Dates;
import cn.ponfee.scheduler.common.exception.Throwables.ThrowingSupplier;
import cn.ponfee.scheduler.common.lock.DoInLocked;
import cn.ponfee.scheduler.core.base.AbstractHeartbeatThread;
import cn.ponfee.scheduler.core.enums.*;
import cn.ponfee.scheduler.core.exception.JobException;
import cn.ponfee.scheduler.core.model.SchedInstance;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.supervisor.instance.TriggerInstanceCreator;
import cn.ponfee.scheduler.supervisor.manager.SchedulerJobManager;
import cn.ponfee.scheduler.supervisor.util.TriggerTimeUtils;
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

import static cn.ponfee.scheduler.core.base.JobConstants.PROCESS_BATCH_SIZE;

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

    private final DoInLocked doInLocked;
    private final SchedulerJobManager schedulerJobManager;
    private final long afterMilliseconds;
    private final ExecutorService processJobExecutor;

    public TriggeringJobScanner(long heartbeatPeriodMilliseconds,
                                int processJobMaximumPoolSize,
                                DoInLocked doInLocked,
                                SchedulerJobManager schedulerJobManager) {
        super(heartbeatPeriodMilliseconds);
        this.doInLocked = doInLocked;
        this.schedulerJobManager = schedulerJobManager;
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
        if (schedulerJobManager.hasNotDiscoveredWorkers()) {
            log.warn("Not discovered worker.");
            return true;
        }
        Boolean result = doInLocked.action(() -> {
            Date now = new Date();
            long maxNextTriggerTime = now.getTime() + afterMilliseconds;
            List<SchedJob> jobs = schedulerJobManager.findBeTriggeringJob(maxNextTriggerTime, PROCESS_BATCH_SIZE);
            if (jobs == null || jobs.isEmpty()) {
                return true;
            }

            jobs.stream()
                .map(job -> CompletableFuture.runAsync(() -> processJob(job, now, maxNextTriggerTime), processJobExecutor))
                .collect(Collectors.toList())
                .forEach(CompletableFuture::join);

            return false;
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
                String reason = "Stop recompute reason: has not next trigger time";
                job.setRemark(reason);
                log.info("{} | {}", reason, job);
                schedulerJobManager.stopJob(job);
                return;
            } else if (job.getNextTriggerTime() > maxNextTriggerTime) {
                schedulerJobManager.updateJobNextTriggerTime(job);
                return;
            }

            // check has available workers
            if (schedulerJobManager.hasNotDiscoveredWorkers(job.getJobGroup())) {
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

            TriggerInstanceCreator<?> creator = TriggerInstanceCreator.of(job.getJobType(), schedulerJobManager);
            creator.createAndDispatch(job, RunType.SCHEDULE, triggerTime);

        } catch (DuplicateKeyException e) {
            if (schedulerJobManager.updateJobNextTriggerTime(job)) {
                log.info("Conflict trigger time: {} | {}", job, e.getMessage());
            } else {
                log.error("Conflict trigger time: {} | {}", job, e.getMessage());
            }
        } catch (JobException | IllegalArgumentException e) {
            log.error(e.getMessage() + ": " + job, e);
            job.setRemark(StringUtils.truncate("Stop reason: " + e.getMessage(), REMARK_MAX_LENGTH));
            job.setNextTriggerTime(null);
            schedulerJobManager.stopJob(job);
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

        SchedInstance lastInstance = schedulerJobManager.getInstance(job.getJobId(), lastTriggerTime, RunType.SCHEDULE.value());
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
                List<SchedTask> tasks = schedulerJobManager.findMediumInstanceTask(instanceId);
                if (schedulerJobManager.hasAliveExecuting(tasks)) {
                    return checkBlockCollisionTrigger(job, Collections.singletonList(lastInstance), collisionStrategy, now);
                } else {
                    // all workers are dead
                    log.info("Collision, all worker dead, terminate the sched instance: {}", instanceId);
                    schedulerJobManager.cancelInstance(instanceId, lastInstance.getWorkflowInstanceId(), Operations.COLLISION_CANCEL);
                    return false;
                }
            case CANCELED:
                List<SchedInstance> list = schedulerJobManager.findUnterminatedRetryInstance(instanceId);
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
                schedulerJobManager.updateJobNextTriggerTime(job);
                return true;
            case SERIAL:
                // 串行执行：更新下一次的扫描时间
                updateNextScanTime(job, now, SCAN_COLLISION_INTERVAL_SECONDS);
                return true;
            case OVERRIDE:
                // 覆盖执行：先取消上一次的执行
                instances.forEach(e -> schedulerJobManager.cancelInstance(e.getInstanceId(), e.getWorkflowInstanceId(), Operations.COLLISION_CANCEL));
                return false;
            default:
                throw new UnsupportedOperationException("Unsupported collision strategy: " + collisionStrategy.name());
        }
    }

    private void updateNextScanTime(SchedJob job, Date now, int delayedSeconds) {
        Date nextScanTime = Dates.plusSeconds(now, delayedSeconds);
        schedulerJobManager.updateJobNextScanTime(job.getJobId(), nextScanTime, job.getVersion());
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

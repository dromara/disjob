/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.thread;

import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.concurrent.AbstractHeartbeatThread;
import cn.ponfee.disjob.common.concurrent.NamedThreadFactory;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.lock.DoInLocked;
import cn.ponfee.disjob.core.enums.*;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.supervisor.instance.TriggerInstanceCreator;
import cn.ponfee.disjob.supervisor.service.DistributedJobManager;
import cn.ponfee.disjob.supervisor.service.DistributedJobQuerier;
import cn.ponfee.disjob.supervisor.util.TriggerTimeUtils;
import com.google.common.math.IntMath;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;

import javax.annotation.PreDestroy;
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

    private static final int SCAN_COLLIDED_INTERVAL_SECONDS = 60;
    private static final int REMARK_MAX_LENGTH = 255;
    private static final int FAILED_SCAN_COUNT_THRESHOLD = 5;

    private final DoInLocked doInLocked;
    private final DistributedJobManager jobManager;
    private final DistributedJobQuerier jobQuerier;
    private final long afterMilliseconds;
    private final ExecutorService processJobExecutor;

    public TriggeringJobScanner(long heartbeatPeriodMilliseconds,
                                int processJobMaximumPoolSize,
                                DoInLocked doInLocked,
                                DistributedJobManager jobManager,
                                DistributedJobQuerier jobQuerier) {
        super(heartbeatPeriodMilliseconds);
        SingletonClassConstraint.constrain(this);

        this.doInLocked = doInLocked;
        this.jobManager = jobManager;
        this.jobQuerier = jobQuerier;
        this.afterMilliseconds = (heartbeatPeriodMs * 3); // 3s * 3 = 9s
        this.processJobExecutor = ThreadPoolExecutors.builder()
            .corePoolSize(1)
            .maximumPoolSize(Math.max(1, processJobMaximumPoolSize))
            .workQueue(new SynchronousQueue<>())
            .keepAliveTimeSeconds(300)
            .rejectedHandler(ThreadPoolExecutors.CALLER_RUNS)
            .threadFactory(NamedThreadFactory.builder().prefix("triggering_job_scanner").priority(Thread.MAX_PRIORITY).build())
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
            List<SchedJob> jobs = jobQuerier.findBeTriggeringJob(maxNextTriggerTime, PROCESS_BATCH_SIZE);
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

    @PreDestroy
    @Override
    public void close() {
        super.close();
        ThreadPoolExecutors.shutdown(processJobExecutor, 1);
    }

    private void processJob(SchedJob job, Date now, long maxNextTriggerTime) {
        try {
            // 重新再计算一次nextTriggerTime
            job.setNextTriggerTime(recomputeNextTriggerTime(job, now));
            if (job.getNextTriggerTime() == null) {
                String reason = "Recompute has not next trigger time";
                job.setRemark(reason);
                log.info("{} | {}", reason, job);
                jobManager.disableJob(job);
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

            // check collided with last schedule
            if (checkBlockCollidedTrigger(job, now)) {
                return;
            }

            long triggerTime = job.getNextTriggerTime();
            refreshNextTriggerTime(job, triggerTime, now);

            TriggerInstanceCreator<?> creator = TriggerInstanceCreator.of(job.getJobType(), jobManager);
            creator.createWithSaveAndDispatch(job, RunType.SCHEDULE, triggerTime);

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
            jobManager.disableJob(job);
        } catch (Throwable t) {
            log.error("Scan trigger job error: " + job, t);
            if (job.getFailedScanCount() >= FAILED_SCAN_COUNT_THRESHOLD) {
                job.setRemark(StringUtils.truncate("Scan over failed: " + t.getMessage(), REMARK_MAX_LENGTH));
                job.setNextTriggerTime(null);
                jobManager.disableJob(job);
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
     * Check is whether block if the next trigger collided
     *
     * @param job the sched job
     * @param now the now date time
     * @return {@code true} will block the next trigger
     */
    private boolean checkBlockCollidedTrigger(SchedJob job, Date now) {
        CollidedStrategy collidedStrategy = CollidedStrategy.of(job.getCollidedStrategy());
        Long lastTriggerTime;
        if (CollidedStrategy.CONCURRENT == collidedStrategy || (lastTriggerTime = job.getLastTriggerTime()) == null) {
            return false;
        }

        SchedInstance lastInstance = jobQuerier.getInstance(job.getJobId(), lastTriggerTime, RunType.SCHEDULE.value());
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
                return checkBlockCollidedTrigger(job, Collections.singletonList(lastInstance), collidedStrategy, now);
            case RUNNING:
                List<SchedTask> tasks = jobQuerier.findBaseInstanceTasks(instanceId);
                if (jobManager.hasAliveExecuting(tasks)) {
                    return checkBlockCollidedTrigger(job, Collections.singletonList(lastInstance), collidedStrategy, now);
                } else {
                    // all workers are dead
                    log.info("All worker dead, terminate collided sched instance: {}", instanceId);
                    jobManager.cancelInstance(lastInstance.obtainLockInstanceId(), Operations.COLLIDED_CANCEL);
                    return false;
                }
            case CANCELED:
                List<SchedInstance> list = jobQuerier.findUnterminatedRetryInstance(instanceId);
                if (CollectionUtils.isEmpty(list)) {
                    return false;
                } else {
                    return checkBlockCollidedTrigger(job, list, collidedStrategy, now);
                }
            default:
                throw new UnsupportedOperationException("Unsupported run state: " + runState.name());
        }
    }

    private boolean checkBlockCollidedTrigger(SchedJob job, List<SchedInstance> instances, CollidedStrategy collidedStrategy, Date now) {
        switch (collidedStrategy) {
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
                    job.setRemark("Disable collided reason: has not next trigger time.");
                    job.setJobState(JobState.DISABLE.value());
                }
                jobManager.updateJobNextTriggerTime(job);
                return true;
            case SERIAL:
                // 串行执行：更新下一次的扫描时间
                updateNextScanTime(job, now, SCAN_COLLIDED_INTERVAL_SECONDS);
                return true;
            case OVERRIDE:
                // 覆盖执行：先取消上一次的执行
                instances.forEach(e -> jobManager.cancelInstance(e.obtainLockInstanceId(), Operations.COLLIDED_CANCEL));
                return false;
            default:
                throw new UnsupportedOperationException("Unsupported collided strategy: " + collidedStrategy.name());
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

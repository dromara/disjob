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

package cn.ponfee.disjob.supervisor.thread;

import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.concurrent.AbstractHeartbeatThread;
import cn.ponfee.disjob.common.concurrent.MultithreadExecutors;
import cn.ponfee.disjob.common.concurrent.NamedThreadFactory;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.lock.LockTemplate;
import cn.ponfee.disjob.core.enums.*;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.supervisor.component.DistributedJobManager;
import cn.ponfee.disjob.supervisor.component.DistributedJobQuerier;
import cn.ponfee.disjob.supervisor.configuration.SupervisorProperties;
import cn.ponfee.disjob.supervisor.instance.TriggerInstanceCreator;
import cn.ponfee.disjob.supervisor.util.TriggerTimeUtils;
import com.google.common.math.IntMath;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;

import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;

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

    private final LockTemplate lockTemplate;
    private final int jobScanFailedCountThreshold;
    private final DistributedJobManager jobManager;
    private final DistributedJobQuerier jobQuerier;
    private final long afterMilliseconds;
    private final ExecutorService processJobExecutor;

    public TriggeringJobScanner(SupervisorProperties supervisorProperties,
                                LockTemplate lockTemplate,
                                DistributedJobManager jobManager,
                                DistributedJobQuerier jobQuerier) {
        super(supervisorProperties.getScanTriggeringJobPeriodMs());
        SingletonClassConstraint.constrain(this);

        this.jobScanFailedCountThreshold = supervisorProperties.getJobScanFailedCountThreshold();
        this.lockTemplate = lockTemplate;
        this.jobManager = jobManager;
        this.jobQuerier = jobQuerier;
        // heartbeat period duration: 3s * 3 = 9s
        this.afterMilliseconds = (heartbeatPeriodMs * 3);
        this.processJobExecutor = ThreadPoolExecutors.builder()
            .corePoolSize(1)
            .maximumPoolSize(Math.max(1, supervisorProperties.getMaximumProcessJobPoolSize()))
            .workQueue(new SynchronousQueue<>())
            .keepAliveTimeSeconds(300)
            .rejectedHandler(ThreadPoolExecutors.CALLER_RUNS)
            .threadFactory(NamedThreadFactory.builder().prefix("triggering_job_scanner").priority(Thread.MAX_PRIORITY).uncaughtExceptionHandler(log).build())
            .build();
    }

    @Override
    protected boolean heartbeat() {
        if (jobManager.hasNotDiscoveredWorkers()) {
            log.warn("Not discovered worker.");
            return true;
        }
        Boolean result = lockTemplate.execute(() -> {
            Date now = new Date();
            long maxNextTriggerTime = now.getTime() + afterMilliseconds;
            List<SchedJob> jobs = jobQuerier.findBeTriggeringJob(maxNextTriggerTime, PROCESS_BATCH_SIZE);
            if (CollectionUtils.isEmpty(jobs)) {
                return true;
            }
            MultithreadExecutors.run(jobs, job -> processJob(job, now, maxNextTriggerTime), processJobExecutor);
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
            // check has available workers
            if (jobManager.hasNotDiscoveredWorkers(job.getGroup())) {
                updateNextScanTime(job, now, 30);
                log.warn("Scan job not discovered worker: {}, {}", job.getJobId(), job.getGroup());
                return;
            }

            // 重新再计算一次nextTriggerTime
            job.setNextTriggerTime(reComputeNextTriggerTime(job, now));
            if (job.getNextTriggerTime() == null) {
                String reason = "Recompute has not next trigger time";
                job.setRemark(reason);
                log.info("{}, {}", reason, job);
                jobManager.disableJob(job);
                return;
            } else if (job.getNextTriggerTime() > maxNextTriggerTime) {
                // 更新next_trigger_time，等待下次扫描
                jobManager.updateJobNextTriggerTime(job);
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
                log.info("Conflict trigger time: {}, {}", job, e.getMessage());
            } else {
                log.error("Conflict trigger time: {}, {}", job, e.getMessage());
            }
        } catch (IllegalArgumentException e) {
            log.error("Scan trigger job failed: " + job, e);
            job.setRemark(StringUtils.truncate("Scan process failed: " + e.getMessage(), REMARK_MAX_LENGTH));
            job.setNextTriggerTime(null);
            jobManager.disableJob(job);
        } catch (Throwable t) {
            log.error("Scan trigger job error: " + job, t);
            if (job.getScanFailedCount() >= jobScanFailedCountThreshold) {
                job.setRemark(StringUtils.truncate("Scan over failed: " + t.getMessage(), REMARK_MAX_LENGTH));
                job.setNextTriggerTime(null);
                jobManager.disableJob(job);
            } else {
                int scanFailedCount = job.incrementAndGetScanFailedCount();
                updateNextScanTime(job, now, IntMath.pow(scanFailedCount, 2) * 5);
            }
        }
    }

    /**
     * 因为可能会有Misfire，这里需要重新计算本次的触发时间
     *
     * @param job the job
     * @param now the current date
     * @return accurate next trigger time milliseconds
     */
    private Long reComputeNextTriggerTime(SchedJob job, Date now) {
        if (TriggerType.FIXED_DELAY.equalsValue(job.getTriggerType())) {
            // 固定延时类型不重新计算nextTriggerTime
            return job.obtainNextTriggerTime();
        }
        if (now.getTime() <= (job.getNextTriggerTime() + afterMilliseconds)) {
            // 没有过期不重新计算nextTriggerTime
            return job.getNextTriggerTime();
        }
        // 其它情况则基于原来的lastTriggerTime重新再计算一次nextTriggerTime
        return TriggerTimeUtils.computeNextTriggerTime(job, now);
    }

    /**
     * 计算下一次的触发时间
     *
     * @param job the job
     * @param now the current date
     * @return newly next trigger time milliseconds
     */
    private static Long doComputeNextTriggerTime(SchedJob job, Date now) {
        if (TriggerType.FIXED_DELAY.equalsValue(job.getTriggerType())) {
            // 固定延时类型的nextTriggerTime：先更新为long最大值，当任务实例运行完成时去主动计算并更新
            // null值已被用作表示没有下次触发时间
            return Long.MAX_VALUE;
        }
        return TriggerTimeUtils.computeNextTriggerTime(job, now);
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
                    jobManager.cancelInstance(lastInstance.obtainLockInstanceId(), Operation.COLLIDED_CANCEL);
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
        if (TriggerType.FIXED_DELAY.equalsValue(job.getTriggerType())) {
            SchedInstance first = instances.get(0);
            log.error("Fixed delay trigger type cannot happen run collided: {}, {}", first.obtainRnstanceId(), job.getNextTriggerTime());
        }
        switch (collidedStrategy) {
            case DISCARD:
                // 丢弃执行：基于当前时间来更新下一次的执行时间
                Integer misfireStrategy = job.getMisfireStrategy();
                try {
                    job.setMisfireStrategy(MisfireStrategy.DISCARD.value());
                    job.setNextTriggerTime(doComputeNextTriggerTime(job, now));
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
                instances.forEach(e -> jobManager.cancelInstance(e.obtainLockInstanceId(), Operation.COLLIDED_CANCEL));
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
        job.setNextTriggerTime(doComputeNextTriggerTime(job, now));
        if (job.getNextTriggerTime() == null) {
            // It has not next triggered time, then stop the job
            job.setRemark("Disable refresh reason: has not next trigger time");
            job.setJobState(JobState.DISABLE.value());
        }
    }

}

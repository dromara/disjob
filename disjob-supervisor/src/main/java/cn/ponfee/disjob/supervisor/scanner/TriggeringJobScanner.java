/*
 * Copyright 2022-2026 Ponfee (http://www.ponfee.cn/)
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

package cn.ponfee.disjob.supervisor.scanner;

import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.concurrent.*;
import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.lock.LockTemplate;
import cn.ponfee.disjob.core.enums.*;
import cn.ponfee.disjob.supervisor.base.TriggerTimes;
import cn.ponfee.disjob.supervisor.component.JobManager;
import cn.ponfee.disjob.supervisor.component.JobQuerier;
import cn.ponfee.disjob.supervisor.component.WorkerClient;
import cn.ponfee.disjob.supervisor.configuration.SupervisorProperties;
import cn.ponfee.disjob.supervisor.model.SchedInstance;
import cn.ponfee.disjob.supervisor.model.SchedJob;
import com.google.common.math.IntMath;
import org.springframework.dao.DuplicateKeyException;

import javax.annotation.PreDestroy;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;

/**
 * The schedule job heartbeat thread, <br/>
 * find the sched_job which will be trigger, <br/>
 * split to one sched_instance and many sched_task
 *
 * @author Ponfee
 */
public class TriggeringJobScanner extends AbstractHeartbeatThread {

    private final int scanBatchSize;
    private final int maximumJobScanFailures;
    private final JobManager jobManager;
    private final JobQuerier jobQuerier;
    private final WorkerClient workerClient;
    private final LockTemplate lockTemplate;
    private final long afterMilliseconds;
    private final ExecutorService processJobExecutor;
    private final PeriodExecutor logPrinter = new PeriodExecutor(30_000L, () -> log.warn("Not discovered any worker."));

    public TriggeringJobScanner(SupervisorProperties conf,
                                JobManager jobManager,
                                JobQuerier jobQuerier,
                                WorkerClient workerClient,
                                LockTemplate lockTemplate) {
        super(conf.getScanTriggeringJobPeriodMs());
        SingletonClassConstraint.constrain(this);

        this.scanBatchSize = conf.getScanBatchSize();
        this.maximumJobScanFailures = conf.getMaximumJobScanFailures();
        this.jobManager = jobManager;
        this.jobQuerier = jobQuerier;
        this.workerClient = workerClient;
        this.lockTemplate = lockTemplate;
        // heartbeat period duration: 2s * 3 = 6s
        this.afterMilliseconds = heartbeatPeriodMs * 3;
        this.processJobExecutor = ThreadPoolExecutors.builder()
            .corePoolSize(1)
            .maximumPoolSize(Math.max(1, conf.getMaximumProcessJobPoolSize()))
            .workQueue(new SynchronousQueue<>())
            .keepAliveTimeSeconds(300)
            .rejectedHandler(ThreadPoolExecutors.CALLER_RUNS)
            .threadFactory(NamedThreadFactory.builder().prefix("triggering_job_scanner").priority(Thread.MAX_PRIORITY).daemon(true).uncaughtExceptionHandler(log).build())
            .build();
    }

    @Override
    protected boolean heartbeat() {
        if (!workerClient.hasAliveWorker()) {
            logPrinter.execute();
            return true;
        }
        return Boolean.TRUE.equals(lockTemplate.execute(this::scan));
    }

    @PreDestroy
    @Override
    public void close() {
        super.close();
        ThreadPoolExecutors.shutdown(processJobExecutor, 1);
    }

    // --------------------------------------------------------------------------private methods

    private boolean scan() {
        long maxNextTriggerTime = System.currentTimeMillis() + afterMilliseconds;
        List<SchedJob> jobs = jobQuerier.findBeTriggeringJob(maxNextTriggerTime, scanBatchSize);
        MultithreadExecutors.run(jobs, this::processJob, processJobExecutor);
        return jobs.size() < scanBatchSize;
    }

    private void processJob(SchedJob job) {
        Date now = new Date();
        try {
            // check has available workers
            if (!workerClient.hasAliveWorker(job.getGroup())) {
                updateNextScanTime(job, now, 60_000L);
                log.warn("Scan job none alive worker: {}, {}", job.getJobId(), job.getGroup());
                return;
            }
            // 重新再计算一次nextTriggerTime
            job.setNextTriggerTime(reComputeNextTriggerTime(job, now));
            if (job.getNextTriggerTime() == null) {
                disableJob(job, "not next trigger time");
                return;
            }
            long triggerTime = job.getNextTriggerTime();
            if (job.getLastTriggerTime() != null && job.getLastTriggerTime() >= triggerTime) {
                disableJob(job, "invalid next trigger time '" + triggerTime + "'");
                return;
            }
            if (triggerTime > (now.getTime() + afterMilliseconds)) {
                // 更新next_trigger_time，等待下次扫描
                jobManager.updateJobNextTriggerTime(job);
                return;
            }
            // check collision with last schedule
            if (shouldBlockCollisionTrigger(job, now)) {
                return;
            }

            refreshNextTriggerTime(job, triggerTime, now);
            jobManager.scheduleTriggerJob(job, triggerTime);
        } catch (DuplicateKeyException e) {
            boolean updated = jobManager.updateJobNextTriggerTime(job);
            log.info("Update conflict next trigger time: {}, {}, {}", updated, job, e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Scan trigger job failed: {}", job, e);
            disableJob(job, "scan process failed, " + e.getMessage());
        } catch (Throwable t) {
            log.error("Scan trigger job error: {}", job, t);
            if (job.getScanFailures() >= maximumJobScanFailures) {
                disableJob(job, "scan over failed, " + t.getMessage());
            } else {
                int scanFailures = job.incrementAndGetScanFailures();
                updateNextScanTime(job, now, IntMath.pow(scanFailures, 2) * 5000L);
            }
        }
    }

    private void disableJob(SchedJob job, String reason) {
        if (jobManager.disableJob(job)) {
            log.warn("Disabled sched job: {}, {}", job.getJobId(), reason);
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
        if (job.isFixedTriggerType()) {
            // 固定延时类型不重新计算nextTriggerTime
            return job.obtainNextTriggerTime();
        }
        if ((job.getNextTriggerTime() + afterMilliseconds) >= now.getTime()) {
            // 没有过期不重新计算nextTriggerTime
            return job.getNextTriggerTime();
        }
        // 其它情况则基于原来的lastTriggerTime重新再计算一次nextTriggerTime
        return TriggerTimes.computeNextTriggerTime(job, now);
    }

    /**
     * 计算下一次的触发时间
     *
     * @param job the job
     * @param now the current date
     * @return newly next trigger time milliseconds
     */
    private static Long doComputeNextTriggerTime(SchedJob job, Date now) {
        if (job.isFixedTriggerType()) {
            // 固定延时类型的nextTriggerTime：先更新为long最大值，当任务实例运行完成时去主动计算并更新
            // null值已被用作表示没有下次触发时间
            return Long.MAX_VALUE;
        }
        return TriggerTimes.computeNextTriggerTime(job, now);
    }

    /**
     * Check is whether block if the next trigger collision.
     *
     * @param job the sched job
     * @param now the now date time
     * @return {@code true} will block the next trigger
     */
    private boolean shouldBlockCollisionTrigger(SchedJob job, Date now) {
        CollisionStrategy collisionStrategy = CollisionStrategy.of(job.getCollisionStrategy());
        Long lastTriggerTime = job.getLastTriggerTime();
        if (CollisionStrategy.CONCURRENT == collisionStrategy || lastTriggerTime == null) {
            return false;
        }
        SchedInstance lastInstance = jobQuerier.getInstance(job.getJobId(), lastTriggerTime, RunType.SCHEDULE);
        if (lastInstance == null || lastInstance.isCompleted()) {
            return false;
        }
        if (!RunStatus.of(lastInstance.getRunStatus()).isTerminal()) {
            blockCollisionTrigger(job, lastInstance, collisionStrategy, now);
            return true;
        }

        // In here, the last instance status is `CANCELED`
        if (lastInstance.isWorkflow() || !Boolean.TRUE.equals(lastInstance.getRetrying())) {
            // 工作流(workflow)的重试不在lead实例上：如果`lead instance`为`CANCELED`状态，则表明整个工作流实例已经全部取消了
            return false;
        } else {
            SchedInstance retryingInstance = jobQuerier.getRetryingInstance(lastInstance.getInstanceId());
            blockCollisionTrigger(job, retryingInstance, collisionStrategy, now);
            return true;
        }
    }

    private void blockCollisionTrigger(SchedJob job, SchedInstance instance, CollisionStrategy strategy, Date now) {
        if (job.isFixedTriggerType()) {
            // job.getNextTriggerTime() != Long.MAX_VALUE
            log.warn("Fixed trigger type instance collision: {}", instance);
        }

        if (job.getNextTriggerTime() > now.getTime()) {
            // 还未到达下次的执行时间
            updateNextScanTime(job, now, job.getNextTriggerTime() - now.getTime());
            return;
        }

        if (strategy == CollisionStrategy.DISCARD) {
            // 丢弃执行：基于当前时间来更新下一次的执行时间
            Integer misfireStrategy = job.getMisfireStrategy();
            job.setMisfireStrategy(MisfireStrategy.SKIP_ALL_LOST.value());
            job.setLastTriggerTime(job.getNextTriggerTime());
            job.setNextTriggerTime(doComputeNextTriggerTime(job, now));
            job.setMisfireStrategy(misfireStrategy);
            if (job.getNextTriggerTime() == null || job.getNextTriggerTime() < now.getTime()) {
                // It has not next triggered time, then stop the job
                disableJob(job, "Disable collision next trigger time: " + job.getNextTriggerTime());
            } else {
                jobManager.updateJobNextTriggerTime(job);
                updateNextScanTime(job, now, job.getNextTriggerTime() - now.getTime());
            }
        } else if (strategy == CollisionStrategy.SEQUENTIAL) {
            // 串行执行：更新下一次的扫描时间
            updateNextScanTime(job, now, 30000L);
        } else if (strategy == CollisionStrategy.OVERRIDE) {
            // 覆盖执行：先取消上一次的执行（或取消上一次的重试实例）
            if (instance != null) {
                jobManager.cancelInstance(instance.getInstanceId(), Operation.COLLISION_CANCEL);
            }
            updateNextScanTime(job, now, 3000L);
        } else {
            throw new UnsupportedOperationException("Unsupported collision strategy: " + strategy);
        }
    }

    private void updateNextScanTime(SchedJob job, Date now, long delayMillis) {
        job.setNextScanTime(Dates.plusMillis(now, delayMillis));
        jobManager.updateJobNextScanTime(job);
    }

    /**
     * Refresh the job next trigger time.
     *
     * @param job         the job
     * @param triggerTime the triggerTime
     * @param now         the current date
     */
    private static void refreshNextTriggerTime(SchedJob job, long triggerTime, Date now) {
        job.setLastTriggerTime(triggerTime);
        job.setNextTriggerTime(doComputeNextTriggerTime(job, now));
        if (job.getNextTriggerTime() == null) {
            // Disable the job if no next trigger time
            job.setJobStatus(JobStatus.DISABLED.value());
        }
    }

}

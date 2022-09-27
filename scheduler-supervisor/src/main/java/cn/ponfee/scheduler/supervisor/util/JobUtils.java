package cn.ponfee.scheduler.supervisor.util;

import cn.ponfee.scheduler.common.date.Dates;
import cn.ponfee.scheduler.core.enums.*;
import cn.ponfee.scheduler.core.exception.JobException;
import cn.ponfee.scheduler.core.handle.JobHandlerUtils;
import cn.ponfee.scheduler.core.handle.JobSplitter;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.core.model.SchedTrack;
import com.google.common.math.IntMath;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

import static cn.ponfee.scheduler.common.date.Dates.max;
import static cn.ponfee.scheduler.core.base.JobCodeMsg.SPLIT_JOB_FAILED;

/**
 * Job utility class
 *
 * @author Ponfee
 */
public final class JobUtils {

    public static Long computeNextTriggerTime(SchedJob job) {
        return computeNextTriggerTime(job, new Date());
    }

    /**
     * Returns the next trigger time
     *
     * @param job     the SchedJob
     * @param current the current date time
     * @return next trigger time milliseconds
     */
    public static Long computeNextTriggerTime(SchedJob job, Date current) {
        TriggerType triggerType;
        if (job == null || (triggerType = TriggerType.of(job.getTriggerType())) == TriggerType.DEPEND) {
            return null;
        }

        MisfireStrategy misfireStrategy = MisfireStrategy.of(job.getMisfireStrategy());
        Date start = job.getStartTime(), last = Dates.ofMillis(job.getLastTriggerTime()), next, base;
        if (triggerType == TriggerType.ONCE) {
            // 1、如果是ONCE则要特殊处理(只执行一次)
            if (last != null) {
                // already executed once, none next time
                return null;
            } else if (misfireStrategy == MisfireStrategy.DISCARD) {
                next = triggerType.computeNextFireTime(job.getTriggerConf(), current);
            } else {
                next = triggerType.computeNextFireTime(job.getTriggerConf(), new Date(0));
            }
        } else if (misfireStrategy == MisfireStrategy.DISCARD || last == null) {
            // 2、如果misfire为丢失策略或这个Job从未触发执行过，则以初始化方式来计算
            base = max(max(last, start), current);
            next = triggerType.computeNextFireTime(job.getTriggerConf(), base);
        } else {
            // 3、如果这个Job有触发执行记录，则基于最近一次调度时间(last_sched_time)来计算

            // 若start被修改则可能会出现：start > last
            base = max(last, start);
            switch (misfireStrategy) {
                case LAST:
                    Date temp = null, recently;
                    do {
                        recently = temp;
                        base = temp = triggerType.computeNextFireTime(job.getTriggerConf(), base);
                    } while (temp != null && temp.before(current));

                    next = recently != null ? recently : temp;
                    break;
                case EVERY:
                    next = triggerType.computeNextFireTime(job.getTriggerConf(), base);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported misfire strategy: " + job.getMisfireStrategy());
            }
        }

        Date end = job.getEndTime();
        next = (next != null && end != null && next.after(end)) ? null : next;

        return next == null ? null : next.getTime();
    }

    /**
     * Builds sched track and sched task list.
     *
     * @param job          the job
     * @param current      the current date time
     * @param uidGenerator the uid generator
     * @throws JobException if occur job exception
     */
    public static Pair<SchedTrack, List<SchedTask>> buildTrackAndTasks(SchedJob job,
                                                                       Date current,
                                                                       LongSupplier uidGenerator) throws JobException {

        // 1、build sched track
        SchedTrack track = new SchedTrack();
        track.setTrackId(uidGenerator.getAsLong());
        track.setJobId(job.getJobId());
        track.setRunType(RunType.SCHEDULE.value());
        track.setTriggerTime(job.getNextTriggerTime());
        track.setRunState(RunState.WAITING.value());
        track.setRetriedCount(0);
        track.setUpdatedAt(current);
        track.setCreatedAt(current);

        // 2、build sched tasks
        List<SchedTask> tasks = splitJob(job).stream()
                                             .map(e -> buildTask(e.getTaskParam(), uidGenerator.getAsLong(), track.getTrackId(), current))
                                             .collect(Collectors.toList());

        return Pair.of(track, tasks);
    }

    /**
     * Builds sched tasks
     *
     * @param taskParam  the task param
     * @param taskId     the task id
     * @param trackId    the track id
     * @param createTime the created time
     * @return SchedTask
     */
    public static SchedTask buildTask(String taskParam, long taskId, long trackId, Date createTime) {
        SchedTask task = new SchedTask(taskParam == null ? "" : taskParam);
        task.setTaskId(taskId);
        task.setTrackId(trackId);
        task.setExecuteState(ExecuteState.WAITING.value());
        task.setUpdatedAt(createTime);
        task.setCreatedAt(createTime);
        return task;
    }

    /**
     * Returns the retry trigger time
     *
     * @param job       the SchedJob
     * @param failCount the failure times
     * @param current   the current date time
     * @return retry trigger time milliseconds
     */
    public static Long computeRetryTriggerTime(SchedJob job, int failCount, Date current) {
        Assert.isTrue(!RetryType.NONE.equals(job.getRetryType()), "Sched job '" + job.getJobId() + "' retry type is NONE.");
        Assert.isTrue(job.getRetryCount() > 0, "Sched job '" + job.getJobId() + "' retry count must greater than 0, but actual " + job.getRetryCount());
        Assert.isTrue(failCount < job.getRetryCount(), "Sched job '" + job.getJobId() + "' retried " + failCount + " exceed " + job.getRetryCount() + " limit.");
        // exponential backoff
        return current.getTime() + job.getRetryInterval() * IntMath.pow(failCount, 2);
    }

    // ----------------------------------------------------------------private methods

    /**
     * Splits job to many sched task.
     *
     * @param job the job
     * @return list of SplitTask
     * @throws JobException if split failed
     */
    private static List<JobSplitter.SplitTask> splitJob(SchedJob job) throws JobException {
        try {
            JobSplitter jobSplitter = JobHandlerUtils.newInstance(job.getJobHandler());

            List<JobSplitter.SplitTask> splitTasks = jobSplitter.split(job);
            if (CollectionUtils.isEmpty(splitTasks)) {
                throw new JobException(SPLIT_JOB_FAILED, "Job split none tasks.");
            }
            return splitTasks;
        } catch (JobException e) {
            throw e;
        } catch (Exception e) {
            throw new JobException(SPLIT_JOB_FAILED, "Split job occur error", e);
        }
    }

}

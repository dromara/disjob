package cn.ponfee.scheduler.supervisor.util;

import cn.ponfee.scheduler.common.date.Dates;
import cn.ponfee.scheduler.core.enums.MisfireStrategy;
import cn.ponfee.scheduler.core.enums.TriggerType;
import cn.ponfee.scheduler.core.model.SchedJob;

import java.util.Date;

import static cn.ponfee.scheduler.common.date.Dates.max;

/**
 * Trigger time utility class
 *
 * @author Ponfee
 */
public final class TriggerTimeUtils {

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

}

/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.util;

import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.core.enums.MisfireStrategy;
import cn.ponfee.disjob.core.enums.TriggerType;
import cn.ponfee.disjob.core.model.SchedJob;

import java.util.Date;

/**
 * Trigger time utility class
 *
 * @author Ponfee
 */
public final class TriggerTimeUtils {

    /**
     * Returns the next trigger time
     *
     * @param job the job data
     * @param now the now time
     * @return next trigger time milliseconds
     */
    public static Long computeNextTriggerTime(SchedJob job, Date now) {
        TriggerType triggerType;
        if (job == null || TriggerType.DEPEND == (triggerType = TriggerType.of(job.getTriggerType()))) {
            return null;
        }

        MisfireStrategy misfireStrategy = MisfireStrategy.of(job.getMisfireStrategy());
        Date last = Dates.ofTimeMillis(job.getLastTriggerTime()), next;
        Date base = Dates.max(job.getStartTime(), last, now);

        if (triggerType == TriggerType.ONCE) {
            // 1、ONCE只执行一次，这里要特殊处理
            if (last != null) {
                // already executed once, not has next time
                next = null;
            } else if (misfireStrategy == MisfireStrategy.DISCARD) {
                next = triggerType.computeNextTriggerTime(job.getTriggerValue(), base);
            } else {
                next = triggerType.computeNextTriggerTime(job.getTriggerValue(), new Date(Long.MIN_VALUE));
            }

        } else if (misfireStrategy == MisfireStrategy.DISCARD || last == null) {
            // 2、如果misfire为 `丢弃策略` 或 `从未触发执行过`，则基于最新的时间来计算
            next = triggerType.computeNextTriggerTime(job.getTriggerValue(), base);

        } else if (misfireStrategy == MisfireStrategy.LAST) {
            // 3、如果这个Job有触发执行记录，则基于最近的一次触发时间(last_trigger_time)来计算
            if (job.getStartTime() != null && job.getStartTime().after(last)) {
                // 若start被修改则可能会出现`start > last`，则基于start
                last = job.getStartTime();
            } else {
                last = triggerType.computeNextTriggerTime(job.getTriggerValue(), last);
            }
            do {
                next = last;
                last = triggerType.computeNextTriggerTime(job.getTriggerValue(), last);
            } while (last != null && last.before(base));

        } else if (misfireStrategy == MisfireStrategy.EVERY) {
            // 4、执行所有misfire
            next = triggerType.computeNextTriggerTime(job.getTriggerValue(), Dates.max(last, job.getStartTime()));

        } else {
            throw new IllegalArgumentException("Unsupported misfire strategy: " + job.getMisfireStrategy());

        }

        return next == null || (job.getEndTime() != null && next.after(job.getEndTime())) ? null : next.getTime();
    }

}

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

package cn.ponfee.disjob.supervisor.base;

import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.core.enums.MisfireStrategy;
import cn.ponfee.disjob.core.enums.TriggerType;
import cn.ponfee.disjob.supervisor.model.SchedJob;
import org.springframework.util.Assert;

import java.util.Date;

/**
 * Trigger time utility
 *
 * @author Ponfee
 */
public final class TriggerTimes {

    /**
     * Returns the next trigger time
     *
     * @param job the job data
     * @param now the now time
     * @return next trigger time milliseconds
     */
    public static Long computeNextTriggerTime(SchedJob job, Date now) {
        Assert.notNull(now, "Compute next trigger time 'now' cannot be null.");
        final Date next = computeNextTriggerTime0(job, now), end = job.getEndTime();
        return next == null || (end != null && next.after(end)) ? null : next.getTime();
    }

    // -------------------------------------------------------------------------------private methods

    private static Date computeNextTriggerTime0(SchedJob job, Date now) {
        TriggerType type;
        if (job == null || TriggerType.DEPEND == (type = TriggerType.of(job.getTriggerType()))) {
            return null;
        }

        MisfireStrategy strategy = MisfireStrategy.of(job.getMisfireStrategy());
        String value = job.getTriggerValue();
        final Date start = job.getStartTime();
        final Date last = Dates.ofTimeMillis(job.getLastTriggerTime());
        final Date max = Dates.max(start, last, now);

        if (type == TriggerType.ONCE) {
            // `ONCE`类型要单独处理
            Date next = type.computeNextTriggerTime(value, new Date(-1));
            boolean isInvalid = (next == null) ||
                (start != null && next.before(start)) ||
                (last != null && !next.after(last)) ||
                (strategy == MisfireStrategy.SKIP_ALL_LOST && next.before(max));
            return isInvalid ? null : next;
        }

        if (max.equals(start) && !start.equals(last)) {
            // last < now <= start[max]
            return type.computeFirstTriggerTime(value, start);
        }

        if (last == null) {
            // start < now[max]
            Date next = type.computeNextTriggerTime(value, max);
            if (strategy == MisfireStrategy.SKIP_ALL_LOST || next != null) {
                return next;
            }
            // 解决某些CRON组件支持固定时间表达式的场景，如(2022-01-02 03:04:05)：5 4 3 2 1 ? 2022
            next = type.computeFirstTriggerTime(value, start != null ? start : new Date(-1));
            if (next == null || (start != null && next.before(start))) {
                return null;
            }
            if (strategy == MisfireStrategy.FIRE_ALL_LOST) {
                return next;
            }
            // 到了这里 misfireStrategy=FIRE_ONCE_NOW
            Date afterNext = type.computeNextTriggerTime(value, next);
            // (next < now < afterNext) ? next : now
            return (afterNext == null || afterNext.after(max)) ? next : max;
        }

        // last < start ? computeFirstTriggerTime(start) : computeNextTriggerTime(last)
        Date next = (start != null && last.before(start)) ?
            type.computeFirstTriggerTime(value, start) : type.computeNextTriggerTime(value, last);
        if (next == null || !next.before(max)) {
            // next == null || next >= max
            return next;
        }

        // ---------------- On here: start < next < now[max] ---------------- //

        if (strategy == MisfireStrategy.FIRE_ALL_LOST) {
            return next;
        } else if (strategy == MisfireStrategy.FIRE_ONCE_NOW) {
            Date afterNext = type.computeNextTriggerTime(value, next);
            // (next < now < afterNext) ? next : now
            return (afterNext == null || afterNext.after(max)) ? next : max;
        } else if (strategy == MisfireStrategy.SKIP_ALL_LOST) {
            return type.computeNextTriggerTime(value, max);
        } else {
            throw new UnsupportedOperationException("Unsupported compute next trigger time: " + type);
        }
    }

}

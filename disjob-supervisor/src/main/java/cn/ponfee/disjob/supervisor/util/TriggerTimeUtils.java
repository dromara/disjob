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

package cn.ponfee.disjob.supervisor.util;

import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.core.enums.MisfireStrategy;
import cn.ponfee.disjob.core.enums.TriggerType;
import cn.ponfee.disjob.core.model.SchedJob;

import java.util.Date;

/**
 * Trigger time utility
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
        final Date next = computeNextTriggerTime0(job, now), end = job.getEndTime();
        return next == null || (end != null && next.after(end)) ? null : next.getTime();
    }

    private static Date computeNextTriggerTime0(SchedJob job, Date now) {
        TriggerType type;
        if (job == null || TriggerType.DEPEND == (type = TriggerType.of(job.getTriggerType()))) {
            return null;
        }

        String value = job.getTriggerValue();
        final Date start = job.getStartTime();
        final Date last = Dates.ofTimeMillis(job.getLastTriggerTime());
        final Date base = Dates.max(start, last, now);

        if (last == null) {
            return type.computeNextTriggerTime(value, base);
        }

        MisfireStrategy strategy = MisfireStrategy.of(job.getMisfireStrategy());
        if (type == TriggerType.ONCE) {
            Date next = type.computeNextTriggerTime(value, new Date(Long.MIN_VALUE));
            if (!next.after(last)) {
                // 不支持执行时间回拨
                return null;
            }
            return (strategy == MisfireStrategy.SKIP_ALL_PAST && next.before(base)) ? null : next;
        } else {
            Date next = type.computeNextTriggerTime(value, last);
            if (next == null || !next.before(base)) {
                return next;
            }
            return strategy == MisfireStrategy.SKIP_ALL_PAST ? type.computeNextTriggerTime(value, base) : base;
        }
    }

}

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

package cn.ponfee.disjob.core.enums;

import cn.ponfee.disjob.common.base.IntValueEnum;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.date.CronExpression;
import cn.ponfee.disjob.common.date.DatePeriodValue;
import cn.ponfee.disjob.common.date.DatePeriods;
import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.util.Jsons;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.TriggerContext;
import org.springframework.util.Assert;

import java.text.ParseException;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * The trigger type enum definition.
 * <p>mapped by sched_job.trigger_type
 *
 * <pre>{@link
 *  org.springframework.scheduling.support.CronExpression#isValidExpression(String)
 * }</pre>
 *
 * <pre>{@link
 *  org.springframework.scheduling.support.CronExpression#next(Temporal)
 * }</pre>
 *
 * <pre>{@link
 *  org.springframework.scheduling.support.CronTrigger#nextExecutionTime(TriggerContext)
 * }</pre>
 *
 * @author Ponfee
 * @see org.springframework.scheduling.support.CronTrigger
 * @see org.springframework.scheduling.support.CronExpression
 */
public enum TriggerType implements IntValueEnum<TriggerType> {

    /**
     * Cron expression<br/>
     * Specified date time of cron exp(2021-12-31 23:59:59): 59 59 23 31 12 ? 2021
     */
    CRON(1, false, "0/10 * * * * ?", "Cron表达式") {
        @Override
        protected boolean validate0(String triggerValue) {
            return CronExpression.isValidExpression(triggerValue);
        }

        @Override
        protected Date computeFirstTriggerTime0(String triggerValue, Date startTime) {
            return computeNextTriggerTime0(triggerValue, Dates.minusMillis(startTime, 1));
        }

        @Override
        protected Date computeNextTriggerTime0(String triggerValue, Date baseTime) {
            try {
                return new CronExpression(triggerValue).getNextValidTimeAfter(baseTime);
            } catch (ParseException e) {
                throw new IllegalArgumentException("Invalid cron expression: " + triggerValue, e);
            }
        }

        @Override
        protected List<Date> computeNextTriggerTimes0(String triggerValue, Date baseTime, int count) {
            try {
                CronExpression cronExpression = new CronExpression(triggerValue);
                List<Date> result = new ArrayList<>(count);
                while (count-- > 0 && (baseTime = cronExpression.getNextValidTimeAfter(baseTime)) != null) {
                    result.add(baseTime);
                }
                return result;
            } catch (ParseException e) {
                throw new IllegalArgumentException("Invalid cron expression: " + triggerValue, e);
            }
        }
    },

    /**
     * 指定执行时间(执行一次)，yyyy-MM-dd HH:mm:ss格式
     *
     * @see java.util.Date
     */
    ONCE(2, false, "2000-01-01 00:00:00", "指定时间") {
        @Override
        protected boolean validate0(String triggerValue) {
            try {
                Dates.DATETIME_FORMAT.parse(triggerValue);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }

        @Override
        protected Date computeFirstTriggerTime0(String triggerValue, Date startTime) {
            return computeNextTriggerTime0(triggerValue, Dates.minusMillis(startTime, 1));
        }

        @Override
        protected Date computeNextTriggerTime0(String triggerValue, Date baseTime) {
            try {
                Date next = Dates.DATETIME_FORMAT.parse(triggerValue);
                return next.after(baseTime) ? next : null;
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid once date value: " + triggerValue, e);
            }
        }

        @Override
        protected List<Date> computeNextTriggerTimes0(String triggerValue, Date baseTime, int count) {
            Date next = computeNextTriggerTime0(triggerValue, baseTime);
            return next == null ? Collections.emptyList() : Collections.singletonList(next);
        }
    },

    /**
     * 指定周期
     *
     * @see DatePeriods
     */
    PERIOD(3, false, "{\"period\":\"DAILY\", \"start\":\"2000-01-01 00:00:00\", \"step\":10}", "指定周期") {
        @Override
        protected boolean validate0(String triggerValue) {
            try {
                DatePeriodValue conf = Jsons.fromJson(triggerValue, DatePeriodValue.class);
                return conf != null && conf.verify();
            } catch (Exception ignored) {
                return false;
            }
        }

        @Override
        protected Date computeFirstTriggerTime0(String triggerValue, Date startTime) {
            return computeNextTriggerTime0(triggerValue, Dates.minusMillis(startTime, 1));
        }

        @Override
        protected Date computeNextTriggerTime0(String triggerValue, Date targetTime) {
            List<Date> list = computeNextTriggerTimes0(triggerValue, targetTime, 1);
            return list.isEmpty() ? null : list.get(0);
        }

        @Override
        protected List<Date> computeNextTriggerTimes0(String triggerValue, Date targetTime, int count) {
            DatePeriodValue conf;
            try {
                conf = Jsons.fromJson(triggerValue, DatePeriodValue.class);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid period config: " + triggerValue, e);
            }
            Assert.isTrue(conf != null && conf.verify(), () -> "Invalid period config: " + triggerValue);

            DatePeriods period = conf.getPeriod();
            Date start = conf.getStart();
            List<Date> result = new ArrayList<>(count);
            Date next = start.after(targetTime) ? start : period.next(start, targetTime, conf.getStep(), 1).begin();
            result.add(next);
            while (--count > 0) {
                result.add(next = period.next(next, conf.getStep(), 1).begin());
            }
            return result;
        }
    },

    /**
     * 指定间隔，如果任务的执行耗时超过间隔时间，则可能会出现当前任务还未执行完(COMPLETED/CANCELED)时，同时开始执行下一个任务
     */
    INTERVAL(4, false, "60", "指定间隔(秒)") {
        @Override
        protected boolean validate0(String triggerValue) {
            return Long.parseLong(triggerValue) > 0;
        }

        @Override
        protected Date computeFirstTriggerTime0(String triggerValue, Date startTime) {
            return new Date(startTime.getTime());
        }

        @Override
        protected Date computeNextTriggerTime0(String triggerValue, Date lastTriggerTime) {
            return computeNextTriggerTimes0(triggerValue, lastTriggerTime, 1).get(0);
        }

        @Override
        protected List<Date> computeNextTriggerTimes0(String triggerValue, Date lastTriggerTime, int count) {
            long interval = Long.parseLong(triggerValue);
            Assert.isTrue(interval > 0, () -> name() + " invalid trigger value: " + triggerValue);
            List<Date> result = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                result.add(lastTriggerTime = Dates.plusSeconds(lastTriggerTime, interval));
            }
            return result;
        }
    },

    /**
     * 固定频率：以上一个任务实例的`计划触发时间`开始计算，在`triggerValue`秒后触发执行下一个任务实例
     * <p>如果任务的执行耗时超过频率时间，则会在当前任务执行完(COMPLETED/CANCELED)后立即执行下一个任务，但不会同时执行
     */
    FIXED_RATE(5, true, "60", "固定频率(秒)") {
        @Override
        protected boolean validate0(String triggerValue) {
            return Long.parseLong(triggerValue) > 0;
        }

        @Override
        protected Date computeFirstTriggerTime0(String triggerValue, Date startTime) {
            return new Date(startTime.getTime());
        }

        @Override
        protected Date computeNextTriggerTime0(String triggerValue, Date lastTriggerTime) {
            long rate = Long.parseLong(triggerValue);
            Assert.isTrue(rate > 0, () -> name() + " invalid trigger value: " + triggerValue);
            return Dates.plusSeconds(lastTriggerTime, rate);
        }

        @Override
        protected List<Date> computeNextTriggerTimes0(String triggerValue, Date lastTriggerTime, int count) {
            throw new UnsupportedOperationException(name() + " unsupported compute multiple next trigger times.");
        }
    },

    /**
     * 固定延时：以上一个任务实例`执行完成时间`(COMPLETED/CANCELED)开始计算，延后`triggerValue`秒后触发执行下一个任务实例
     */
    FIXED_DELAY(6, true, "60", "固定延时(秒)") {
        @Override
        protected boolean validate0(String triggerValue) {
            return Long.parseLong(triggerValue) > 0;
        }

        @Override
        protected Date computeFirstTriggerTime0(String triggerValue, Date startTime) {
            return new Date(startTime.getTime());
        }

        @Override
        protected Date computeNextTriggerTime0(String triggerValue, Date lastCompletedTime) {
            long delay = Long.parseLong(triggerValue);
            Assert.isTrue(delay > 0, () -> name() + " invalid trigger value: " + triggerValue);
            return Dates.plusSeconds(lastCompletedTime, delay);
        }

        @Override
        protected List<Date> computeNextTriggerTimes0(String triggerValue, Date lastCompletedTime, int count) {
            throw new UnsupportedOperationException(name() + " unsupported compute multiple next trigger times.");
        }
    },

    /**
     * 任务依赖：依赖父任务执行完再触发执行子任务(trigger_value为父任务job_id，多个逗号分隔)
     */
    DEPEND(7, false, "1003164910267351000,1003164910267351001", "任务依赖") {
        @Override
        protected boolean validate0(String triggerValue) {
            try {
                return !Collects.split(triggerValue, Long::parseLong).isEmpty();
            } catch (NumberFormatException ignored) {
                return false;
            }
        }

        @Override
        protected Date computeFirstTriggerTime0(String triggerValue, Date startTime) {
            throw new UnsupportedOperationException(name() + " unsupported compute first trigger time.");
        }

        @Override
        protected Date computeNextTriggerTime0(String triggerValue, Date baseTime) {
            throw new UnsupportedOperationException(name() + " unsupported compute one next trigger time.");
        }

        @Override
        protected List<Date> computeNextTriggerTimes0(String triggerValue, Date baseTime, int count) {
            throw new UnsupportedOperationException(name() + " unsupported compute multiple next trigger times.");
        }
    },

    ;

    private final int value;
    private final boolean fixedTriggerType;
    private final String example;
    private final String desc;

    TriggerType(int value, boolean fixedTriggerType, String example, String desc) {
        this.value = value;
        this.fixedTriggerType = fixedTriggerType;
        this.example = example;
        this.desc = desc;
    }

    @Override
    public final int value() {
        return value;
    }

    @Override
    public final String desc() {
        return desc;
    }

    public final boolean isFixedTriggerType() {
        return fixedTriggerType;
    }

    public final String example() {
        return example;
    }

    protected abstract boolean validate0(String triggerValue);

    protected abstract Date computeFirstTriggerTime0(String triggerValue, Date startTime);

    protected abstract Date computeNextTriggerTime0(String triggerValue, Date startTime);

    protected abstract List<Date> computeNextTriggerTimes0(String triggerValue, Date startTime, int count);

    public final boolean validate(String triggerValue) {
        return StringUtils.isNotBlank(triggerValue) && validate0(triggerValue);
    }

    public final Date computeFirstTriggerTime(String triggerValue, Date date) {
        Assert.notNull(date, "Param date cannot be null.");
        return computeFirstTriggerTime0(triggerValue, date);
    }

    public final Date computeNextTriggerTime(String triggerValue, Date date) {
        Assert.notNull(date, "Param date cannot be null.");
        Date next = computeNextTriggerTime0(triggerValue, date);
        if (next == null || next.after(date)) {
            return next;
        }
        throw new IllegalStateException(name() + " invalid next after: " + triggerValue + ", " + Dates.format(date));
    }

    public final List<Date> computeNextTriggerTimes(String triggerValue, Date date, int count) {
        Assert.notNull(date, "Param date cannot be null.");
        List<Date> list = computeNextTriggerTimes0(triggerValue, date, count);
        if (list.isEmpty()) {
            return list;
        }

        Date curr, next = date;
        for (Date item : list) {
            curr = next;
            next = item;
            Assert.isTrue(next.after(curr), () -> name() + " invalid next after: " + triggerValue + ", " + Dates.format(date));
        }
        return list;
    }

    public static TriggerType of(int value) {
        for (TriggerType e : VALUES) {
            if (e.value() == value) {
                return e;
            }
        }
        throw new IllegalArgumentException("Invalid trigger type value: " + value);
    }

    private static final TriggerType[] VALUES = TriggerType.values();

}

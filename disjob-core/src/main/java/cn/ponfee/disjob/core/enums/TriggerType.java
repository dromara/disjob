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
import cn.ponfee.disjob.common.date.CronExpression;
import cn.ponfee.disjob.common.date.DatePeriods;
import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.util.Enums;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.model.PeriodTriggerValue;
import cn.ponfee.disjob.core.model.SchedDepend;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.text.ParseException;
import java.util.*;

/**
 * The trigger type enum definition.
 * <p>mapped by sched_job.trigger_type
 *
 * <pre>{@code
 *  org.springframework.scheduling.support.CronExpression.isValidExpression(cronExpression)
 * }</pre>
 *
 * <pre>{@code
 *  new org.springframework.scheduling.support.CronExpression(cronExpression).next(date)
 * }</pre>
 *
 * <pre>{@code
 *  new org.springframework.scheduling.support.CronTrigger(cronExpression).nextExecutionTime(triggerContext);
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
    CRON(1, "0/10 * * * * ?", "Cron表达式") {
        @Override
        public boolean validate0(String triggerValue) {
            return CronExpression.isValidExpression(triggerValue);
        }

        @Override
        public Date computeNextTriggerTime(String triggerValue, Date startTime) {
            try {
                return new CronExpression(triggerValue).getNextValidTimeAfter(startTime);
            } catch (ParseException e) {
                throw new IllegalArgumentException("Invalid cron expression: " + triggerValue, e);
            }
        }

        @Override
        public List<Date> computeNextTriggerTimes(String triggerValue, Date startTime, int count) {
            List<Date> result = new ArrayList<>(count);
            CronExpression cronExpression;
            try {
                cronExpression = new CronExpression(triggerValue);
            } catch (ParseException e) {
                throw new IllegalArgumentException("Invalid cron expression: " + triggerValue, e);
            }
            while (count-- > 0 && (startTime = cronExpression.getNextValidTimeAfter(startTime)) != null) {
                result.add(startTime);
            }
            return result;
        }
    },

    /**
     * 指定执行时间(执行一次)，yyyy-MM-dd HH:mm:ss格式
     *
     * @see java.util.Date
     */
    ONCE(2, "2000-01-01 00:00:00", "指定时间") {
        @Override
        public boolean validate0(String triggerValue) {
            try {
                Dates.DATETIME_FORMAT.parse(triggerValue);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }

        @Override
        public Date computeNextTriggerTime(String triggerValue, Date startTime) {
            try {
                Date dateTime = Dates.DATETIME_FORMAT.parse(triggerValue);
                return dateTime.after(startTime) ? dateTime : null;
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid once date format: " + triggerValue, e);
            }
        }

        @Override
        public List<Date> computeNextTriggerTimes(String triggerValue, Date startTime, int count) {
            Assert.isTrue(count == 1, () -> name() + " unsupported compute multiple next trigger time: " + count);
            Date nextTriggerTime = computeNextTriggerTime(triggerValue, startTime);
            return nextTriggerTime == null ? Collections.emptyList() : Collections.singletonList(nextTriggerTime);
        }
    },

    /**
     * 周期性执行
     *
     * @see DatePeriods
     */
    PERIOD(3, "{\"period\":\"DAILY\", \"start\":\"2000-01-01 00:00:00\", \"step\":1}", "固定周期") {
        @Override
        public boolean validate0(String triggerValue) {
            try {
                PeriodTriggerValue conf = Jsons.fromJson(triggerValue, PeriodTriggerValue.class);
                return conf != null && conf.verify();
            } catch (Exception ignored) {
                return false;
            }
        }

        @Override
        public Date computeNextTriggerTime(String triggerValue, Date startTime) {
            List<Date> list = computeNextTriggerTimes(triggerValue, startTime, 1);
            if (CollectionUtils.isEmpty(list)) {
                return null;
            }
            Assert.isTrue(list.size() == 1, () -> name() + " compute too many next trigger time.");
            return list.get(0);
        }

        @Override
        public List<Date> computeNextTriggerTimes(String triggerValue, Date startTime, int count) {
            PeriodTriggerValue conf;
            try {
                conf = Jsons.fromJson(triggerValue, PeriodTriggerValue.class);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid period config: " + triggerValue, e);
            }
            Assert.isTrue(conf != null && conf.verify(), () -> "Invalid period config: " + triggerValue);

            DatePeriods period = conf.getPeriod();
            Date start = conf.getStart(), next;
            List<Date> result = new ArrayList<>(count);

            result.add(next = start.after(startTime) ? start : period.next(start, startTime, conf.getStep(), 1).begin());
            while (--count > 0) {
                result.add(next = period.next(next, conf.getStep(), 1).begin());
            }
            return result;
        }
    },

    /**
     * 固定频率：以上一个任务实例的`计划触发时间`开始计算，在`triggerValue`秒后触发执行下一个任务实例
     */
    FIXED_RATE(4, "60", "固定频率(秒)") {
        @Override
        public boolean validate0(String triggerValue) {
            return Long.parseLong(triggerValue) > 0;
        }

        @Override
        public Date computeNextTriggerTime(String triggerValue, Date lastTriggerTime) {
            return computeNextTriggerTimes(triggerValue, lastTriggerTime, 1).get(0);
        }

        @Override
        public List<Date> computeNextTriggerTimes(String triggerValue, Date lastTriggerTime, int count) {
            long period = Long.parseLong(triggerValue);
            Assert.isTrue(period > 0, () -> name() + " invalid trigger value: " + triggerValue);
            List<Date> result = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                result.add(lastTriggerTime = Dates.plusSeconds(lastTriggerTime, period));
            }
            return result;
        }
    },

    /**
     * 固定延时：以上一个任务实例`执行完成时间`开始计算，延后`triggerValue`秒后触发执行下一个任务实例
     */
    FIXED_DELAY(5, "60", "固定延时(秒)") {
        @Override
        public boolean validate0(String triggerValue) {
            return Long.parseLong(triggerValue) > 0;
        }

        @Override
        public Date computeNextTriggerTime(String triggerValue, Date previousCompletedTime) {
            long delay = Long.parseLong(triggerValue);
            Assert.isTrue(delay > 0, () -> name() + " invalid trigger value: " + triggerValue);
            return Dates.plusSeconds(previousCompletedTime, delay);
        }

        @Override
        public List<Date> computeNextTriggerTimes(String triggerValue, Date lastCompletedTime, int count) {
            Assert.isTrue(count == 1, () -> name() + " unsupported compute multiple next trigger time: " + count);
            return Collections.singletonList(computeNextTriggerTime(triggerValue, lastCompletedTime));
        }
    },

    /**
     * 任务依赖：依赖父任务执行完再触发执行子任务(trigger_value为父任务job_id，多个逗号分隔)
     */
    DEPEND(6, "1003164910267351000,1003164910267351001", "任务依赖") {
        @Override
        public boolean validate0(String triggerValue) {
            try {
                return !SchedDepend.parseTriggerValue(triggerValue).isEmpty();
            } catch (NumberFormatException ignored) {
                return false;
            }
        }

        @Override
        public Date computeNextTriggerTime(String triggerValue, Date startTime) {
            throw new UnsupportedOperationException(name() + " unsupported compute one next trigger time.");
        }

        @Override
        public List<Date> computeNextTriggerTimes(String triggerValue, Date startTime, int count) {
            throw new UnsupportedOperationException(name() + " unsupported compute multiple next trigger time.");
        }
    },

    ;

    private final int value;
    private final String example;
    private final String desc;

    TriggerType(int value, String example, String desc) {
        this.value = value;
        this.example = example;
        this.desc = desc;
    }

    @Override
    public int value() {
        return value;
    }

    @Override
    public String desc() {
        return desc;
    }

    public String example() {
        return example;
    }

    public final boolean validate(String triggerValue) {
        return StringUtils.isNotBlank(triggerValue) && validate0(triggerValue);
    }

    protected abstract boolean validate0(String triggerValue);

    public abstract Date computeNextTriggerTime(String triggerValue, Date startTime);

    public abstract List<Date> computeNextTriggerTimes(String triggerValue, Date startTime, int count);

    public static TriggerType of(Integer value) {
        return Objects.requireNonNull(Const.MAPPING.get(value), () -> "Invalid trigger type value: " + value);
    }

    public static final class Const {
        private static final Map<Integer, TriggerType> MAPPING = Enums.toMap(TriggerType.class, TriggerType::value);
        public static final List<TriggerType> FIXED_TYPES = ImmutableList.of(FIXED_RATE, FIXED_DELAY);
    }

}

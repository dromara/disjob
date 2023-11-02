/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.enums;

import cn.ponfee.disjob.common.base.IntValueEnum;
import cn.ponfee.disjob.common.base.Symbol.Str;
import cn.ponfee.disjob.common.date.CronExpression;
import cn.ponfee.disjob.common.date.DatePeriods;
import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.util.Enums;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.model.PeriodTriggerValue;
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
        public Date computeNextFireTime(String triggerValue, Date startTime) {
            try {
                return new CronExpression(triggerValue).getNextValidTimeAfter(startTime);
            } catch (ParseException e) {
                throw new IllegalArgumentException("Invalid cron expression: " + triggerValue, e);
            }
        }

        @Override
        public List<Date> computeNextFireTimes(String triggerValue, Date startTime, int count) {
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
        public Date computeNextFireTime(String triggerValue, Date startTime) {
            try {
                Date dateTime = Dates.DATETIME_FORMAT.parse(triggerValue);
                return dateTime.after(startTime) ? dateTime : null;
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid once date format: " + triggerValue, e);
            }
        }

        @Override
        public List<Date> computeNextFireTimes(String triggerValue, Date startTime, int count) {
            Assert.isTrue(count == 1, () -> name() + " unsupported compute multiple next fire time: " + count);
            Date nextFireTime = computeNextFireTime(triggerValue, startTime);
            return nextFireTime == null ? Collections.emptyList() : Collections.singletonList(nextFireTime);
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
                return conf != null && conf.isValid();
            } catch (Exception ignored) {
                return false;
            }
        }

        @Override
        public Date computeNextFireTime(String triggerValue, Date startTime) {
            List<Date> list = computeNextFireTimes(triggerValue, startTime, 1);
            if (CollectionUtils.isEmpty(list)) {
                return null;
            }
            Assert.isTrue(list.size() == 1, () -> name() + " compute too many next fire time.");
            return list.get(0);
        }

        @Override
        public List<Date> computeNextFireTimes(String triggerValue, Date startTime, int count) {
            PeriodTriggerValue conf;
            try {
                conf = Jsons.fromJson(triggerValue, PeriodTriggerValue.class);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid period config: " + triggerValue, e);
            }
            Assert.isTrue(conf != null && conf.isValid(), () -> "Invalid period config: " + triggerValue);

            DatePeriods period = conf.getPeriod();
            List<Date> result = new ArrayList<>(count);
            Date next;
            if (conf.getStart().after(startTime)) {
                next = conf.getStart();
            } else {
                next = period.next(conf.getStart(), startTime, conf.getStep(), 1).begin();
            }
            result.add(next);
            count--;

            while (count-- > 0) {
                result.add(next = period.next(next, conf.getStep(), 1).begin());
            }
            return result;
        }
    },

    /**
     * 固定频率：以上一个任务实例的触发时间开始计算，固定在triggerValue毫秒后触发执行下一个任务实例
     */
    FIXED_RATE(4, "60000", "固定频率(毫秒)") {
        @Override
        public boolean validate0(String triggerValue) {
            return Long.parseLong(triggerValue) > 0;
        }

        @Override
        public Date computeNextFireTime(String triggerValue, Date previousFireTime) {
            return Dates.plusMillis(previousFireTime, Long.parseLong(triggerValue));
        }

        @Override
        public List<Date> computeNextFireTimes(String triggerValue, Date previousFireTime, int count) {
            List<Date> result = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                result.add(previousFireTime = Dates.plusMillis(previousFireTime, Long.parseLong(triggerValue)));
            }
            return result;
        }
    },

    /**
     * 固定延时：以上一个任务实例执行完成时间开始计算，延后triggerValue毫秒后触发执行下一个任务实例
     */
    FIXED_DELAY(5, "60000", "固定延时(毫秒)") {
        @Override
        public boolean validate0(String triggerValue) {
            return Long.parseLong(triggerValue) > 0;
        }

        @Override
        public Date computeNextFireTime(String triggerValue, Date previousFinishedTime) {
            return Dates.plusMillis(previousFinishedTime, Long.parseLong(triggerValue));
        }

        @Override
        public List<Date> computeNextFireTimes(String triggerValue, Date previousFinishedTime, int count) {
            Assert.isTrue(count == 1, () -> name() + " unsupported compute multiple next fire time: " + count);
            return Collections.singletonList(computeNextFireTime(triggerValue, previousFinishedTime));
        }
    },

    /**
     * 任务依赖：依赖父任务执行完再触发执行子任务(trigger_value为父任务job_id，多个逗号分隔)
     */
    DEPEND(6, "1003164910267351000,1003164910267351001", "任务依赖") {
        @Override
        public boolean validate0(String triggerValue) {
            try {
                long count = Arrays.stream(triggerValue.split(Str.COMMA))
                    .filter(StringUtils::isNotBlank)
                    .map(String::trim)
                    .map(Long::parseLong)
                    .filter(e -> e > 0)
                    .count();
                return count > 0;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }

        @Override
        public Date computeNextFireTime(String triggerValue, Date startTime) {
            throw new UnsupportedOperationException(name() + " unsupported compute one next fire time.");
        }

        @Override
        public List<Date> computeNextFireTimes(String triggerValue, Date startTime, int count) {
            throw new UnsupportedOperationException(name() + " unsupported compute multiple next fire time.");
        }
    },

    ;

    private static final Map<Integer, TriggerType> MAPPING = Enums.toMap(TriggerType.class, TriggerType::value);

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

    public abstract Date computeNextFireTime(String triggerValue, Date startTime);

    public abstract List<Date> computeNextFireTimes(String triggerValue, Date startTime, int count);

    public static TriggerType of(Integer value) {
        return Objects.requireNonNull(MAPPING.get(value), () -> "Invalid trigger type value: " + value);
    }

}

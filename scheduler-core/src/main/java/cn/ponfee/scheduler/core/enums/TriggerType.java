/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.enums;

import cn.ponfee.scheduler.common.base.IntValue;
import cn.ponfee.scheduler.common.base.Symbol.Str;
import cn.ponfee.scheduler.common.date.CronExpression;
import cn.ponfee.scheduler.common.date.DatePeriods;
import cn.ponfee.scheduler.common.date.Dates;
import cn.ponfee.scheduler.common.util.Enums;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.model.PeriodTriggerValue;
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
public enum TriggerType implements IntValue<TriggerType> {

    /**
     * Cron expression<br/>
     * Specified date time of cron exp(2021-12-31 23:59:59): 59 59 23 31 12 ? 2021
     */
    CRON(1, "0/10 * * * * ?") {
        @Override
        public boolean isValid(String triggerValue) {
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
    ONCE(2, "2000-01-01 00:00:00") {
        @Override
        public boolean isValid(String triggerValue) {
            try {
                Dates.toDate(triggerValue);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }

        @Override
        public Date computeNextFireTime(String triggerValue, Date startTime) {
            try {
                Date dateTime = Dates.toDate(triggerValue);
                return dateTime.after(startTime) ? dateTime : null;
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid once date format: " + triggerValue, e);
            }
        }

        @Override
        public List<Date> computeNextFireTimes(String triggerValue, Date startTime, int count) {
            Date nextFireTime = computeNextFireTime(triggerValue, startTime);
            return nextFireTime == null ? Collections.emptyList() : Collections.singletonList(nextFireTime);
        }
    },

    /**
     * 周期性执行
     *
     * @see DatePeriods
     */
    PERIOD(3, "{\"period\":\"DAILY\", \"start\":\"2000-01-01 00:00:00\", \"step\":1}") {
        @Override
        public boolean isValid(String triggerValue) {
            try {
                PeriodTriggerValue conf = Jsons.fromJson(triggerValue, PeriodTriggerValue.class);
                return conf != null && conf.isValid();
            } catch (Exception ignored) {
                return false;
            }
        }

        @Override
        public Date computeNextFireTime(String triggerValue, Date startTime) {
            return getOne(computeNextFireTimes(triggerValue, startTime, 1));
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
     * 任务依赖：依赖父任务执行完再触发执行子任务(trigger_value为父任务job_id，多个逗号分隔)
     */
    DEPEND(4, "3988904755200,3988904755201") {
        @Override
        public boolean isValid(String triggerValue) {
            if (StringUtils.isBlank(triggerValue)) {
                return false;
            }
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
            throw new UnsupportedOperationException("Trigger type 'DEPEND' unsupported.");
        }

        @Override
        public List<Date> computeNextFireTimes(String triggerValue, Date startTime, int count) {
            throw new UnsupportedOperationException("Trigger type 'DEPEND' unsupported.");
        }
    },

    ;

    private static final Map<Integer, TriggerType> MAPPING = Enums.toMap(TriggerType.class, TriggerType::value);

    private final int value;
    private final String example;

    TriggerType(int value, String example) {
        this.value = value;
        this.example = example;
    }

    @Override
    public int value() {
        return value;
    }

    public String example() {
        return example;
    }

    public abstract boolean isValid(String triggerValue);

    public abstract Date computeNextFireTime(String triggerValue, Date startTime);

    public abstract List<Date> computeNextFireTimes(String triggerValue, Date startTime, int count);

    public static TriggerType of(Integer value) {
        TriggerType triggerType = MAPPING.get(value);
        Assert.notNull(triggerType, () -> "Invalid trigger type value: " + value);
        return triggerType;
    }

    private static <T> T getOne(List<T> list) {
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        Assert.isTrue(list.size() == 1, () -> "The list expect one size, but actual is " + list.size());
        return list.get(0);
    }

}

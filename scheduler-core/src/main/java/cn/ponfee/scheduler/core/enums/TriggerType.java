package cn.ponfee.scheduler.core.enums;

import cn.ponfee.scheduler.common.base.Constants;
import cn.ponfee.scheduler.common.date.CronExpression;
import cn.ponfee.scheduler.common.date.DatePeriods;
import cn.ponfee.scheduler.common.date.Dates;
import cn.ponfee.scheduler.common.util.Enums;
import cn.ponfee.scheduler.core.model.PeriodTriggerConf;
import com.alibaba.fastjson.JSON;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The trigger type enum definition.
 * <p>mapped by sched_job.trigger_type
 *
 * <pre>{@code
 *  org.springframework.scheduling.support.CronSequenceGenerator.isValidExpression(cronExpression)
 * }</pre>
 *
 * <pre>{@code
 *  new org.springframework.scheduling.support.CronSequenceGenerator(cronExpression).next(date)
 * }</pre>
 *
 * <pre>{@code
 *  new org.springframework.scheduling.support.CronTrigger(cronExpression).nextExecutionTime(triggerContext);
 * }</pre>
 *
 * @author Ponfee
 * @see org.springframework.scheduling.support.CronTrigger
 * @see org.springframework.scheduling.support.CronSequenceGenerator
 */
public enum TriggerType {

    /**
     * Cron expression<br/>
     * Specified date time of cron exp(2021-12-31 23:59:59): 59 59 23 31 12 ? 2021
     */
    CRON(1, "0/10 * * * * ?") {
        @Override
        public boolean isValid(String triggerConf) {
            return CronExpression.isValidExpression(triggerConf);
        }

        @Override
        public Date computeNextFireTime(String triggerConf, Date startTime) {
            try {
                return new CronExpression(triggerConf).getNextValidTimeAfter(startTime);
            } catch (ParseException e) {
                throw new IllegalArgumentException("Invalid cron expression: " + triggerConf, e);
            }
        }

        @Override
        public List<Date> computeNextFireTimes(String triggerConf, Date startTime, int count) {
            List<Date> result = new ArrayList<>(count);
            CronExpression cronExpression;
            try {
                cronExpression = new CronExpression(triggerConf);
            } catch (ParseException e) {
                throw new IllegalArgumentException("Invalid cron expression: " + triggerConf, e);
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
        public boolean isValid(String triggerConf) {
            try {
                Dates.toDate(triggerConf);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }

        @Override
        public Date computeNextFireTime(String triggerConf, Date startTime) {
            try {
                Date dateTime = Dates.toDate(triggerConf);
                return dateTime.after(startTime) ? dateTime : null;
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid once date format: " + triggerConf, e);
            }
        }

        @Override
        public List<Date> computeNextFireTimes(String triggerConf, Date startTime, int count) {
            Date nextFireTime = computeNextFireTime(triggerConf, startTime);
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
        public boolean isValid(String triggerConf) {
            try {
                PeriodTriggerConf conf = JSON.parseObject(triggerConf, PeriodTriggerConf.class);
                return conf != null && conf.isValid();
            } catch (Exception ignored) {
                return false;
            }
        }

        @Override
        public Date computeNextFireTime(String triggerConf, Date startTime) {
            return getOne(computeNextFireTimes(triggerConf, startTime, 1));
        }

        @Override
        public List<Date> computeNextFireTimes(String triggerConf, Date startTime, int count) {
            PeriodTriggerConf conf;
            try {
                conf = JSON.parseObject(triggerConf, PeriodTriggerConf.class);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid period config: " + triggerConf, e);
            }
            Assert.isTrue(conf != null && conf.isValid(), "Invalid period config: " + triggerConf);

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
     * 任务依赖：依赖父任务执行完再触发执行子任务(trigger_conf为父任务job_id，多个逗号分隔)
     */
    DEPEND(4, "1534764645317890107,1534764645317890108") {
        @Override
        public boolean isValid(String triggerConf) {
            if (StringUtils.isBlank(triggerConf)) {
                return false;
            }
            try {
                List<Long> list = Arrays.stream(triggerConf.split(Constants.COMMA))
                                        .filter(StringUtils::isNotBlank)
                                        .map(e -> Long.parseLong(e.trim()))
                                        .collect(Collectors.toList());
                return !list.isEmpty() && list.stream().allMatch(e -> e > 0);
            } catch (NumberFormatException ignored) {
                return false;
            }
        }

        @Override
        public Date computeNextFireTime(String triggerConf, Date startTime) {
            return null;
        }

        @Override
        public List<Date> computeNextFireTimes(String triggerConf, Date startTime, int count) {
            return null;
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

    public int value() {
        return value;
    }

    public String example() {
        return example;
    }

    public abstract boolean isValid(String triggerConf);

    public abstract Date computeNextFireTime(String triggerConf, Date startTime);

    public abstract List<Date> computeNextFireTimes(String triggerConf, Date startTime, int count);

    public boolean equals(Integer value) {
        return value != null && this.value == value;
    }

    public static TriggerType of(int value) {
        TriggerType triggerType = MAPPING.get(value);
        if (triggerType == null) {
            throw new IllegalArgumentException("Invalid trigger type: " + value);
        }
        return triggerType;
    }

    private static <T> T getOne(List<T> list) {
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        Assert.isTrue(list.size() == 1, "The list except one size, but actual " + list.size());
        return list.get(0);
    }
}

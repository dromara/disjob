/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.date;

import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.function.ToIntFunction;

/**
 * <pre>
 * 1990-04-15 00:00:00这天调整了夏令时，即在4月15号0点的时候将表调快了一小时，导致这一天少了一小时。
 *
 * 1986年4月，中国中央有关部门发出“在全国范围内实行夏时制的通知”，具体作法是：每年从四月中旬第一个星
 * 期日的凌晨2时整（北京时间），将时钟拨快一小时，即将表针由2时拨至3时，夏令时开始；到九月中旬第一个
 * 星期日的凌晨2时整（北京夏令时），再将时钟拨回一小时，即将表针由2时拨至1时，夏令时结束。从1986年到
 * 1991年的六个年度，除1986年因是实行夏时制的第一年，从5月4日开始到9月14日结束外，其它年份均按规定的
 * 时段施行。在夏令时开始和结束前几天，新闻媒体均刊登有关部门的通告。1992年起，夏令时暂停实行。
 *
 * 时间周期，计算周期性的时间段
 * </pre>
 *
 * @author Ponfee
 */
public enum DatePeriods {

    /**
     * 每秒钟的
     */
    PER_SECOND(PeriodType.seconds(), Period::getSeconds, LocalDateTime::plusSeconds, 1),

    /**
     * 每分钟的
     */
    MINUTELY(PeriodType.minutes(), Period::getMinutes, LocalDateTime::plusMinutes, 1),

    /**
     * 每小时的
     */
    HOURLY(PeriodType.hours(), Period::getHours, LocalDateTime::plusHours, 1),

    /**
     * 每天
     */
    DAILY(PeriodType.days(), Period::getDays, LocalDateTime::plusDays, 1),

    /**
     * 每周
     */
    WEEKLY(PeriodType.weeks(), Period::getWeeks, LocalDateTime::plusWeeks, 1),

    /**
     * 每月
     */
    MONTHLY(PeriodType.months(), Period::getMonths, LocalDateTime::plusMonths, 1),

    /**
     * 每季度
     */
    QUARTERLY(PeriodType.months(), Period::getMonths, LocalDateTime::plusMonths, 3),

    /**
     * 每半年
     */
    SEMIANNUAL(PeriodType.months(), Period::getMonths, LocalDateTime::plusMonths, 6),

    /**
     * 每年度
     */
    ANNUAL(PeriodType.years(), Period::getYears, LocalDateTime::plusYears, 1);

    /**
     * 2018-01-01: the first day of year, month, week
     */
    public static final String ORIGINAL_DATE_TIME = "2018-01-01 00:00:00.000";
    //private static final LocalDateTime ORIGINAL = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS").parseLocalDateTime(ORIGINAL_DATE_TIME);

    private final PeriodType type;
    private final ToIntFunction<Period> unit;
    private final PlusFunction plus;
    private final int multiple;

    DatePeriods(PeriodType periodType, ToIntFunction<Period> unit, PlusFunction plus, int multiple) {
        this.type = periodType;
        this.unit = unit;
        this.plus = plus;
        this.multiple = multiple;
    }

    /**
     * Compute the next segment based original and reference target
     *
     * @param original the period original
     * @param target   the target of next reference
     * @param step     the period step
     * @param next     the next of target segment
     * @return {@code Segment(begin, end)}
     */
    public final Segment next(LocalDateTime original, LocalDateTime target, int step, int next) {
        Assert.isTrue(step > 0, "Step must be positive number.");
        Assert.isTrue(!original.isAfter(target), "Original date cannot be after target date.");
        // original.withTime(original.getHourOfDay(), 0, 0, 0)
        // original.withMillisOfDay(0)
        // target.withMillisOfDay(0)

        Period period = new Period(original, target, type);
        step *= multiple;
        LocalDateTime begin = plus.apply(original, (unit.applyAsInt(period) / step + next) * step);
        return new Segment(begin, plus.apply(begin, step));
    }

    public final Segment next(LocalDateTime target, int step, int next) {
        return next(target, target, step, next);
    }

    public final Segment next(LocalDateTime target, int next) {
        return next(target, target, 1, next);
    }

    public final Segment next(Date original, Date target, int step, int next) {
        return next(new LocalDateTime(original), new LocalDateTime(target), step, next);
    }

    public final Segment next(Date target, int step, int next) {
        LocalDateTime original = new LocalDateTime(target);
        return next(original, original, step, next);
    }

    public final Segment next(Date target, int next) {
        LocalDateTime original = new LocalDateTime(target);
        return next(original, original, 1, next);
    }

    public static final class Segment {
        private final Date begin;
        private final Date end;

        private Segment(LocalDateTime begin, LocalDateTime end) {
            this.begin = begin.toDate();
            this.end = end.minusMillis(1).toDate();
        }

        public Date begin() {
            return begin;
        }

        public Date end() {
            return end;
        }

        @Override
        public String toString() {
            return JavaUtilDateFormat.PATTERN_51.format(begin) + " ~ " + JavaUtilDateFormat.PATTERN_51.format(end);
        }
    }

    private interface PlusFunction {
        LocalDateTime apply(LocalDateTime dateTime, int value);
    }
}

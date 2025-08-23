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

package cn.ponfee.disjob.common.date;

import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

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
     * 每毫秒的
     */
    PER_MILLISECOND(ChronoUnit.MILLIS, 1),

    /**
     * 每秒钟的
     */
    PER_SECOND(ChronoUnit.SECONDS, 1),

    /**
     * 每分钟的
     */
    MINUTELY(ChronoUnit.MINUTES, 1),

    /**
     * 每小时的
     */
    HOURLY(ChronoUnit.HOURS, 1),

    /**
     * 每天
     */
    DAILY(ChronoUnit.DAYS, 1),

    /**
     * 每周
     */
    WEEKLY(ChronoUnit.WEEKS, 1),

    /**
     * 每月
     */
    MONTHLY(ChronoUnit.MONTHS, 1),

    /**
     * 每季度
     */
    QUARTERLY(ChronoUnit.MONTHS, 3),

    /**
     * 每半年
     */
    SEMIANNUAL(ChronoUnit.MONTHS, 6),

    /**
     * 每年度
     */
    ANNUAL(ChronoUnit.YEARS, 1),

    /**
     * 每十年的
     */
    DECENNIAL(ChronoUnit.DECADES, 1),

    /**
     * 每百年的（世纪）
     */
    CENTENNIAL(ChronoUnit.CENTURIES, 1),

    ;

    private final ChronoUnit unit;
    private final int multiple;

    DatePeriods(ChronoUnit unit, int multiple) {
        this.unit = unit;
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

        step *= multiple;
        long start = (unit.between(original, target) / step + next) * step;
        LocalDateTime begin = original.plus(start, unit);
        return new Segment(begin, begin.plus(step, unit));
    }

    public final Segment next(LocalDateTime target, int step, int next) {
        return next(target, target, step, next);
    }

    public final Segment next(LocalDateTime target, int next) {
        return next(target, target, 1, next);
    }

    public final Segment next(Date original, Date target, int step, int next) {
        return next(Dates.toLocalDateTime(original), Dates.toLocalDateTime(target), step, next);
    }

    public final Segment next(Date target, int step, int next) {
        LocalDateTime original = Dates.toLocalDateTime(target);
        return next(original, original, step, next);
    }

    public final Segment next(Date target, int next) {
        LocalDateTime original = Dates.toLocalDateTime(target);
        return next(original, original, 1, next);
    }

    public static final class Segment {
        private final Date begin;
        private final Date end;

        private Segment(LocalDateTime begin, LocalDateTime end) {
            this.begin = Dates.toDate(begin);
            this.end = Dates.toDate(end.minus(1, ChronoUnit.MILLIS));
        }

        public Date begin() {
            return begin;
        }

        public Date end() {
            return end;
        }

        @Override
        public String toString() {
            return Dates.DATETIME_MILLI_FORMAT.format(begin) + " ~ " + Dates.DATETIME_MILLI_FORMAT.format(end);
        }
    }

}

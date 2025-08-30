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

import cn.ponfee.disjob.common.base.Symbol.Char;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Date utility
 * <p><a href="https://segmentfault.com/a/1190000039047353">Java处理GMT/UTC日期时间</a>
 *
 * <pre>
 * UTC本身不包含时区偏移（即偏移量为0，实际上就是UTC+0）
 * UTC时间格式：2000-01-01T01:23:45.123456789Z，其中Z表示`Zulu Time`，Z等价于`+00:00`
 *
 * 时区：
 *   LocalDateTime：无时区
 *   Date(UTC+0)：表示自格林威治时间(GMT)1970年1月1日0点经过指定的毫秒数后的时间点
 *   Instant(UTC+0)：同Date
 *   ZonedDateTime：自带完整的时区ID（如Asia/Shanghai），支持夏令时（DST）自动调整
 *   OffsetDateTime：自带偏移量时区（固定的UTC偏移量），不支持夏令时
 *
 * abstract class ZoneId子类：ZoneRegion、ZoneOffset
 *   ZoneId.of("Etc/GMT-8")                   -->    Etc/GMT-8
 *   ZoneId.of("GMT+8")                       -->    GMT+08:00
 *   ZoneId.of("UTC+8")                       -->    UTC+08:00
 *   ZoneId.of("Asia/Shanghai")               -->    Asia/Shanghai
 *   ZoneId.systemDefault()                   -->    Asia/Shanghai
 *
 * TimeZone子类（不支持UTC）：ZoneInfo
 *   TimeZone.getTimeZone("Etc/GMT-8")        -->    Etc/GMT-8
 *   TimeZone.getTimeZone("GMT+8")            -->    GMT+08:00
 *   TimeZone.getTimeZone("Asia/Shanghai")    -->    Asia/Shanghai
 *   TimeZone.getTimeZone(ZoneId.of("GMT+8")) -->    GMT+08:00
 *   TimeZone.getDefault()                    -->    Asia/Shanghai
 * </pre>
 *
 * @author Ponfee
 */
public class Dates {

    /**
     * Time pattern
     */
    public static final String TIME_PATTERN = "HH:mm:ss";

    /**
     * Date pattern
     */
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * Datetime pattern
     */
    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * Datetime milli pattern
     */
    public static final String DATETIME_MILLI_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * Date compact pattern
     */
    public static final String DATE_COMPACT_PATTERN = "yyyyMMdd";

    /**
     * Datetime compact pattern
     */
    public static final String DATETIME_COMPACT_PATTERN = "yyyyMMddHHmmss";

    /**
     * Datetime milli compact pattern
     */
    public static final String DATETIME_MILLI_COMPACT_PATTERN = "yyyyMMddHHmmssSSS";

    /**
     * Date pattern of {@link Date#toString()}
     */
    public static final String DATE_TO_STRING_PATTERN = "EEE MMM dd HH:mm:ss zzz yyyy";

    /**
     * Fast date format for date pattern
     */
    public static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance(DATE_PATTERN);

    /**
     * Fast date format for datetime pattern
     */
    public static final FastDateFormat DATETIME_FORMAT = FastDateFormat.getInstance(DATETIME_PATTERN);

    /**
     * Fast date format for datetime milli pattern
     */
    public static final FastDateFormat DATETIME_MILLI_FORMAT = FastDateFormat.getInstance(DATETIME_MILLI_PATTERN);

    /**
     * Fast date format for date compact pattern
     */
    public static final FastDateFormat DATE_COMPACT_FORMAT = FastDateFormat.getInstance(DATE_COMPACT_PATTERN);

    /**
     * Fast date format for datetime compact pattern
     */
    public static final FastDateFormat DATETIME_COMPACT_FORMAT = FastDateFormat.getInstance(DATETIME_COMPACT_PATTERN);

    /**
     * Fast date format for datetime milli compact pattern
     */
    public static final FastDateFormat DATETIME_MILLI_COMPACT_FORMAT = FastDateFormat.getInstance(DATETIME_MILLI_COMPACT_PATTERN);

    /**
     * 简单的日期格式校验
     *
     * @param dateStr 输入日期，如(yyyy-MM-dd)
     * @param pattern 日期格式
     * @return 有效返回true, 反之false
     */
    public static boolean isValidDate(String dateStr, String pattern) {
        if (StringUtils.isBlank(dateStr)) {
            return false;
        }
        try {
            new SimpleDateFormat(pattern).parse(dateStr);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 字符串解析为Date
     *
     * @param dateStr 日期字符串
     * @param pattern 日期格式
     * @return 日期对象
     */
    public static Date parse(String dateStr, String pattern) {
        try {
            return new SimpleDateFormat(pattern).parse(dateStr);
        } catch (ParseException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * 格式化日期对象
     *
     * @param date    日期对象
     * @param pattern 日期格式
     * @return 当前日期字符串
     */
    public static String format(Date date, String pattern) {
        return date != null ? new SimpleDateFormat(pattern).format(date) : null;
    }

    /**
     * 格式化日期对象，格式为yyyy-MM-dd HH:mm:ss
     *
     * @param date 日期对象
     * @return 日期字符串
     */
    public static String format(Date date) {
        return date != null ? DATETIME_FORMAT.format(date) : null;
    }

    public static String formatDuration(long durationMillis) {
        if (durationMillis < 1_000_000L) {
            // 范围：[0.000s ~ 999.9s]
            return formatDouble(durationMillis / 1_000.0D) + "s";
        } else if (durationMillis < 60_000_000L) {
            // 范围：[16.66m ~ 999.9m]
            return formatDouble(durationMillis / 60_000.0D) + "m";
        } else if (durationMillis < 3_600_000_000L) {
            // 范围：[16.66h ~ 999.9h]
            return formatDouble(durationMillis / 3_600_000.0D) + "h";
        } else if (durationMillis < 86_400_000_000L) {
            // 范围：[41.66d ~ 999.9d]
            return formatDouble(durationMillis / 86_400_000.0D) + "d";
        } else {
            // 其它：[1000d ~ xxxxxd]
            return (durationMillis / 86_400_000L) + "d";
        }
    }

    // ----------------------------------------------------------------plus

    /**
     * 增加毫秒数
     *
     * @param date   时间
     * @param millis 毫秒数
     * @return 时间
     */
    public static Date plusMillis(Date date, long millis) {
        return new Date(date.getTime() + millis);
    }

    /**
     * 增加秒数
     *
     * @param date    时间
     * @param seconds 秒数
     * @return 时间
     */
    public static Date plusSeconds(Date date, long seconds) {
        return plusMillis(date, seconds * 1000);
    }

    /**
     * 增加分钟
     *
     * @param date    时间
     * @param minutes 分钟数
     * @return 时间
     */
    public static Date plusMinutes(Date date, long minutes) {
        return plusMillis(date, minutes * 60 * 1000);
    }

    /**
     * 增加小时
     *
     * @param date  时间
     * @param hours 小时数
     * @return 时间
     */
    public static Date plusHours(Date date, long hours) {
        return plusMillis(date, hours * 60 * 60 * 1000);
    }

    /**
     * 增加天数
     *
     * @param date 时间
     * @param days 天数
     * @return 时间
     */
    public static Date plusDays(Date date, long days) {
        return plusMillis(date, days * 24 * 60 * 60 * 1000);
    }

    /**
     * 增加周
     *
     * @param date  时间
     * @param weeks 周数
     * @return 时间
     */
    public static Date plusWeeks(Date date, long weeks) {
        return plusMillis(date, weeks * 7 * 24 * 60 * 60 * 1000);
    }

    /**
     * 增加月份
     *
     * @param date   时间
     * @param months 月数
     * @return 时间
     */
    public static Date plusMonths(Date date, long months) {
        return toDate(toLocalDateTime(date).plusMonths(months));
    }

    /**
     * 增加年
     *
     * @param date  时间
     * @param years 年数
     * @return 时间
     */
    public static Date plusYears(Date date, long years) {
        return toDate(toLocalDateTime(date).plusYears(years));
    }

    // ----------------------------------------------------------------minus

    /**
     * 减少毫秒数
     *
     * @param date   时间
     * @param millis 毫秒数
     * @return 时间
     */
    public static Date minusMillis(Date date, long millis) {
        return plusMillis(date, -millis);
    }

    /**
     * 减少秒数
     *
     * @param date    时间
     * @param seconds 秒数
     * @return 时间
     */
    public static Date minusSeconds(Date date, long seconds) {
        return plusSeconds(date, -seconds);
    }

    /**
     * 减少分钟
     *
     * @param date    时间
     * @param minutes 分钟数
     * @return 时间
     */
    public static Date minusMinutes(Date date, long minutes) {
        return plusMinutes(date, -minutes);
    }

    /**
     * 减少小时
     *
     * @param date  时间
     * @param hours 小时数
     * @return 时间
     */
    public static Date minusHours(Date date, long hours) {
        return plusHours(date, -hours);
    }

    /**
     * 减少天数
     *
     * @param date 时间
     * @param days 天数
     * @return 时间
     */
    public static Date minusDays(Date date, long days) {
        return plusDays(date, -days);
    }

    /**
     * 减少周
     *
     * @param date  时间
     * @param weeks 周数
     * @return 时间
     */
    public static Date minusWeeks(Date date, long weeks) {
        return plusWeeks(date, -weeks);
    }

    /**
     * 减少月份
     *
     * @param date   时间
     * @param months 月数
     * @return 时间
     */
    public static Date minusMonths(Date date, long months) {
        return toDate(toLocalDateTime(date).minusMonths(months));
    }

    /**
     * 减少年
     *
     * @param date  时间
     * @param years 年数
     * @return 时间
     */
    public static Date minusYears(Date date, long years) {
        return toDate(toLocalDateTime(date).minusYears(years));
    }

    // ----------------------------------------------------------------start/end

    /**
     * 获取指定日期所在天的开始时间：yyyy-MM-dd 00:00:00
     *
     * @param date 时间
     * @return 时间
     */
    public static Date startOfDay(Date date) {
        return toDate(startOfDay0(date));
    }

    /**
     * 获取指定日期所在天的结束时间：yyyy-MM-dd 23:59:59
     *
     * @param date 时间
     * @return 时间
     */
    public static Date endOfDay(Date date) {
        return toDate(endOfDay0(date));
    }

    /**
     * 获取指定日期所在周的开始时间：yyyy-MM-周一 00:00:00
     *
     * @param date 日期
     * @return 当前周第一天
     */
    public static Date startOfWeek(Date date) {
        return toDate(startOfDay0(date).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
    }

    /**
     * 获取指定日期所在周的结束时间：yyyy-MM-周日 23:59:59
     *
     * @param date 日期
     * @return 当前周最后一天
     */
    public static Date endOfWeek(Date date) {
        return toDate(endOfDay0(date).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
    }

    /**
     * 获取指定日期所在月的开始时间：yyyy-MM-01 00:00:00
     *
     * @param date 日期
     * @return 当前月的第一天
     */
    public static Date startOfMonth(Date date) {
        return toDate(startOfDay0(date).with(TemporalAdjusters.firstDayOfMonth()));
    }

    /**
     * 获取指定日期所在月的结束时间：yyyy-MM-月未 23:59:59
     *
     * @param date 日期
     * @return 当前月的最后一天
     */
    public static Date endOfMonth(Date date) {
        return toDate(endOfDay0(date).with(TemporalAdjusters.lastDayOfMonth()));
    }

    /**
     * 获取指定日期所在月的开始时间：yyyy-01-01 00:00:00
     *
     * @param date 日期
     * @return 当前年的第一天
     */
    public static Date startOfYear(Date date) {
        return toDate(startOfDay0(date).with(TemporalAdjusters.firstDayOfYear()));
    }

    /**
     * 获取指定日期所在月的结束时间：yyyy-12-31 23:59:59
     *
     * @param date 日期
     * @return 当前年的最后一天
     */
    public static Date endOfYear(Date date) {
        return toDate(endOfDay0(date).with(TemporalAdjusters.lastDayOfYear()));
    }

    // ----------------------------------------------------------------with day of xxx

    /**
     * 获取指定时间所在周的周n：[1, 7]
     *
     * @param date      相对日期
     * @param dayOfWeek 1-星期一；2-星期二；...
     * @return 本周周几的日期对象
     */
    public static Date withDayOfWeek(Date date, int dayOfWeek) {
        LocalDateTime dateTime = toLocalDateTime(date).with(WeekFields.of(DayOfWeek.MONDAY, 1).dayOfWeek(), dayOfWeek);
        return toDate(dateTime);
    }

    /**
     * 获取指定时间所在月的n号：[1, 31]
     *
     * @param date       the date
     * @param dayOfMonth the day of month
     * @return date
     */
    public static Date withDayOfMonth(Date date, int dayOfMonth) {
        return toDate(toLocalDateTime(date).withDayOfMonth(dayOfMonth));
    }

    /**
     * 获取指定时间所在年的n天，1<=day<=366
     *
     * @param date      the date
     * @param dayOfYear the day of year
     * @return date
     */
    public static Date withDayOfYear(Date date, int dayOfYear) {
        return toDate(toLocalDateTime(date).withDayOfYear(dayOfYear));
    }

    // ----------------------------------------------------------------day of xxx

    public static int dayOfYear(Date date) {
        return toLocalDateTime(date).getDayOfYear();
    }

    public static int dayOfMonth(Date date) {
        return toLocalDateTime(date).getDayOfMonth();
    }

    public static int dayOfWeek(Date date) {
        return toLocalDateTime(date).getDayOfWeek().getValue();
    }

    public static int hourOfDay(Date date) {
        return toLocalDateTime(date).getHour();
    }

    // ----------------------------------------------------------------others

    /**
     * Returns a days between the two date(end-start)
     *
     * @param start the start date
     * @param end   the end date
     * @return a number of between start to end days
     * @see java.time.temporal.ChronoUnit#between(Temporal, Temporal)
     */
    public static int daysBetween(Date start, Date end) {
        //return ChronoUnit.DAYS.between(toLocalDate(start), toLocalDate(end));
        return (int) (toLocalDate(end).toEpochDay() - toLocalDate(start).toEpochDay());
    }

    /**
     * 日期随机
     *
     * @param begin 开发日期
     * @param end   结束日期
     * @return date
     */
    public static Date random(Date begin, Date end) {
        return random(begin.getTime(), end.getTime());
    }

    public static Date random(long beginTimeMillis, long endTimeMillis) {
        long datetimeDiff = endTimeMillis - beginTimeMillis;
        if (datetimeDiff <= 0) {
            throw new IllegalArgumentException("Date begin must before end: " + beginTimeMillis + ", " + endTimeMillis);
        }
        return new Date(beginTimeMillis + ThreadLocalRandom.current().nextLong(datetimeDiff));
    }

    /**
     * Returns the smaller of two {@code Date} values.
     *
     * @param a the first Date
     * @param b the second Date
     * @return the smallest of {@code a} and {@code b}
     */
    public static Date min(Date a, Date b) {
        if (a == null) {
            return b;
        }
        return (b == null || a.before(b)) ? a : b;
    }

    /**
     * Returns the greater of two {@code Date} values.
     *
     * @param a the first Date
     * @param b the second Date
     * @return the greatest of {@code a} and {@code b}
     */
    public static Date max(Date a, Date b) {
        if (a == null) {
            return b;
        }
        return (b == null || a.after(b)) ? a : b;
    }

    public static Date max(Date a, Date b, Date c) {
        return max(max(a, b), c);
    }

    // ----------------------------------------------------------------java 8 date

    public static LocalDateTime startOfDay(LocalDateTime dateTime) {
        return dateTime.with(LocalTime.MIN);
    }

    public static LocalDateTime endOfDay(LocalDateTime dateTime) {
        return dateTime.with(LocalTime.MAX);
    }

    public static LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDateTime toUTC(LocalDateTime systemLocalDateTime) {
        return convertZone(systemLocalDateTime, ZoneId.systemDefault(), ZoneOffset.UTC);
    }

    public static LocalDateTime fromUTC(LocalDateTime utcLocalDateTime) {
        return convertZone(utcLocalDateTime, ZoneOffset.UTC, ZoneId.systemDefault());
    }

    public static LocalDateTime convertZone(LocalDateTime systemLocalDateTime, ZoneId targetZone) {
        return convertZone(systemLocalDateTime, ZoneId.systemDefault(), targetZone);
    }

    /**
     * 转换时区：转换同一UTC时间在不同时区的本地时间
     *
     * @param localDateTime the local date time
     * @param sourceZone    the source zone id
     * @param targetZone    the target zone id
     * @return localDateTime of target zone id
     */
    public static LocalDateTime convertZone(LocalDateTime localDateTime, ZoneId sourceZone, ZoneId targetZone) {
        if (localDateTime == null || sourceZone.equals(targetZone)) {
            return localDateTime;
        }
        return localDateTime.atZone(sourceZone).withZoneSameInstant(targetZone).toLocalDateTime();
    }

    public static String toCronExpression(Date date) {
        return toCronExpression(toLocalDateTime(date));
    }

    /**
     * Converts date time to cron expression
     *
     * @param dateTime the local date time
     * @return cron expression of the spec date
     */
    @SuppressWarnings("StringBufferReplaceableByString")
    public static String toCronExpression(LocalDateTime dateTime) {
        return new StringBuilder(21)
            .append(dateTime.getSecond()    ).append(Char.SPACE) // second
            .append(dateTime.getMinute()    ).append(Char.SPACE) // minute
            .append(dateTime.getHour()      ).append(Char.SPACE) // hour
            .append(dateTime.getDayOfMonth()).append(Char.SPACE) // day
            .append(dateTime.getMonthValue()).append(Char.SPACE) // month
            .append('?'                     ).append(Char.SPACE) // week
            .append(dateTime.getYear()      )                    // year
            .toString();
    }

    // -------------------------------------------------------------private methods

    private static String formatDouble(double value) {
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.DOWN).toString().substring(0, 5);
    }

    private static LocalDateTime startOfDay0(Date date) {
        return startOfDay(toLocalDateTime(date));
    }

    private static LocalDateTime endOfDay0(Date date) {
        return endOfDay(toLocalDateTime(date));
    }

}

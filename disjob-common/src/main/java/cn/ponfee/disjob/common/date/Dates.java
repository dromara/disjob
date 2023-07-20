/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.date;

import cn.ponfee.disjob.common.base.Symbol.Char;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.FastDateFormat;

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
 * 时区：
 *   LocalDateTime：无时区
 *   Date(UTC0)：表示自格林威治时间(GMT)1970年1月1日0点经过指定的毫秒数后的时间点
 *   Instant(UTC0)：同Date
 *   ZonedDateTime：自带时区
 *
 * ZoneId子类：ZoneRegion、ZoneOffset
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
     * Date pattern
     */
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * Datetime pattern
     */
    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * Full datetime pattern
     */
    public static final String DATEFULL_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * Date format of {@link Date#toString()}
     */
    public static final String DATE_TO_STRING_PATTERN = "EEE MMM dd HH:mm:ss zzz yyyy";

    /**
     * Zero time millis: -62170185600000L
     */
    public static final String ZERO_DATETIME = "0000-00-00 00:00:00";

    /**
     * Fast date format for datetime pattern
     */
    public static final FastDateFormat DATETIME_FORMAT = FastDateFormat.getInstance(Dates.DATETIME_PATTERN);

    /**
     * 简单的日期格式校验
     *
     * @param dateStr 输入日期，如(yyyy-MM-dd)
     * @param pattern 日期格式
     * @return 有效返回true, 反之false
     */
    public static boolean isValidDate(String dateStr, String pattern) {
        if (StringUtils.isEmpty(dateStr)) {
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
     * Check the date is whether zero date
     *
     * @param date the date
     * @return is zero if {@code true}
     */
    public static boolean isZeroDate(Date date) {
        return date != null && date.getTime() == -62170185600000L;
    }

    /**
     * 获取当前日期对象
     *
     * @return 当前日期对象
     */
    public static Date now() {
        return new Date();
    }

    /**
     * 获取当前日期字符串
     *
     * @param pattern 日期格式
     * @return 当前日期字符串
     */
    public static String now(String pattern) {
        return format(now(), pattern);
    }

    /**
     * 转换日期即字符串为Date对象
     *
     * @param dateStr 日期字符串
     * @param pattern 日期格式
     * @return 日期对象
     */
    public static Date toDate(String dateStr, String pattern) {
        try {
            return new SimpleDateFormat(pattern).parse(dateStr);
        } catch (ParseException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * java（毫秒）时间戳
     *
     * @param timeMillis 毫秒时间戳
     * @return 日期
     */
    public static Date ofTimeMillis(long timeMillis) {
        return new Date(timeMillis);
    }

    public static Date ofTimeMillis(Long timeMillis) {
        return timeMillis == null ? null : new Date(timeMillis);
    }

    public static long currentUnixTimestamp() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * unix时间戳
     *
     * @param unixTimestamp unix时间戳
     * @return 日期
     */
    public static Date ofUnixTimestamp(long unixTimestamp) {
        return new Date(unixTimestamp * 1000);
    }

    public static Date ofUnixTimestamp(Long unixTimestamp) {
        return unixTimestamp == null ? null : new Date(unixTimestamp * 1000);
    }

    /**
     * 格式化日期对象
     *
     * @param date    日期对象
     * @param pattern 日期格式
     * @return 当前日期字符串
     */
    public static String format(Date date, String pattern) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat(pattern).format(date);
    }

    /**
     * 格式化日期对象，格式为yyyy-MM-dd HH:mm:ss
     *
     * @param date 日期对象
     * @return 日期字符串
     */
    public static String format(Date date) {
        if (date == null) {
            return null;
        }
        return DATETIME_FORMAT.format(date);
    }

    /**
     * 格式化日期对象
     *
     * @param timeMillis 毫秒
     * @param pattern    格式
     * @return 日期字符串
     */
    public static String format(long timeMillis, String pattern) {
        return format(new Date(timeMillis), pattern);
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

    // ----------------------------------------------------------------day of

    /**
     * 获取指定时间所在周的周n，1<=day<=7
     *
     * @param date      相对日期
     * @param dayOfWeek 1-星期一；2-星期二；...
     * @return 本周周几的日期对象
     */
    public static Date withDayOfWeek(Date date, int dayOfWeek) {
        LocalDateTime dateTime = toLocalDateTime(date).with(WeekFields.of(DayOfWeek.MONDAY, 1).dayOfWeek(), dayOfWeek);
        //LocalDateTime dateTime = toLocalDateTime(date).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).with(TemporalAdjusters.nextOrSame(DayOfWeek.of(dayOfWeek)));
        return toDate(dateTime);
    }

    /**
     * 获取指定时间所在月的n号，1<=day<=31
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

    // ----------------------------------------------------------------day of

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
     * 计算两个日期的时间差（单位：秒）
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 时间间隔
     */
    public static long clockDiff(Date start, Date end) {
        return (end.getTime() - start.getTime()) / 1000;
    }

    /**
     * Returns a days between the two date(end-start)
     *
     * @param start the start date
     * @param end   the end date
     * @return a number of between start to end days
     * @see java.time.temporal.ChronoUnit#between(Temporal, Temporal)
     */
    public static int daysBetween(Date start, Date end) {
        return (int) (toLocalDate(end).toEpochDay() - toLocalDate(start).toEpochDay());
    }

    /**
     * 日期随机
     *
     * @param begin 开发日期
     * @param end   结束日期
     * @return
     */
    public static Date random(Date begin, Date end) {
        long beginMills = begin.getTime(), endMills = end.getTime();
        if (beginMills >= endMills) {
            throw new IllegalArgumentException("Date [" + format(begin) + "] must before [" + format(end) + "]");
        }
        return random(beginMills, endMills);
    }

    public static Date random(long beginTimeMills, long endTimeMills) {
        if (beginTimeMills >= endTimeMills) {
            throw new IllegalArgumentException("Date [" + beginTimeMills + "] must before [" + endTimeMills + "]");
        }

        return new Date(beginTimeMills + ThreadLocalRandom.current().nextLong(endTimeMills - beginTimeMills));
    }

    /**
     * Returns the smaller of two {@code Date} values.
     *
     * @param a the first Date
     * @param b the second Date
     * @return the smallest of {@code a} and {@code b}
     */
    public static Date min(Date a, Date b) {
        return a == null ? b : (b == null || a.before(b)) ? a : b;
    }

    /**
     * Returns the greater of two {@code Date} values.
     *
     * @param a the first Date
     * @param b the second Date
     * @return the greatest of {@code a} and {@code b}
     */
    public static Date max(Date a, Date b) {
        return a == null ? b : (b == null || a.after(b)) ? a : b;
    }

    // ----------------------------------------------------------------java 8 date

    public static LocalDateTime toLocalDateTime(Date date) {
        //return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDateTime startOfDay(LocalDateTime dateTime) {
        return LocalDateTime.of(dateTime.toLocalDate(), LocalTime.MIN);
        //return dateTime.withHour(0).withMinute(0).withSecond(0).withNano(0);
    }

    public static LocalDateTime endOfDay(LocalDateTime dateTime) {
        // 当毫秒数大于499时，如果Mysql的datetime字段没有毫秒位数，数据会自动加1秒，所以此处毫秒为000
        return LocalDateTime.of(dateTime.toLocalDate(), LocalTime.of(23, 59, 59, /*999_999_999*/0));
    }

    /**
     * 时区转换
     *
     * @param date       the date
     * @param sourceZone the source zone id
     * @param targetZone the target zone id
     * @return date of target zone id
     */
    public static Date zoneConvert(Date date, ZoneId sourceZone, ZoneId targetZone) {
        if (date == null || sourceZone.equals(targetZone)) {
            return date;
        }
        return Date.from(
            date.toInstant().atZone(targetZone).withZoneSameLocal(sourceZone).toInstant()
        );
    }

    /**
     * 时区转换
     *
     * @param localDateTime the localDateTime
     * @param sourceZone    the source zone id
     * @param targetZone    the target zone id
     * @return localDateTime of target zone id
     */
    public static LocalDateTime zoneConvert(LocalDateTime localDateTime, ZoneId sourceZone, ZoneId targetZone) {
        if (localDateTime == null || sourceZone.equals(targetZone)) {
            return localDateTime;
        }
        return ZonedDateTime.of(localDateTime, sourceZone).withZoneSameInstant(targetZone).toLocalDateTime();
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
    public static String toCronExpression(LocalDateTime dateTime) {
        return new StringBuilder(22)
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

    private static LocalDateTime startOfDay0(Date date) {
        return startOfDay(toLocalDateTime(date));
    }

    private static LocalDateTime endOfDay0(Date date) {
        return endOfDay(toLocalDateTime(date));
    }

}

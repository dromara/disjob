package cn.ponfee.scheduler.common.date;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;

import javax.annotation.Nonnull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

import static cn.ponfee.scheduler.common.util.Strings.BLANK_CHAR;

/**
 * Date utility based joda
 *
 * @author Ponfee
 */
public class Dates {

    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final String ZERO_DATE_TIME = "0000-00-00 00:00:00";

    /**
     * 简单的日期格式校验(yyyy-MM-dd HH:mm:ss)
     *
     * @param date 输入日期
     * @return 有效返回true, 反之false
     */
    public static boolean isValidDate(String date) {
        return isValidDate(date, DEFAULT_DATE_FORMAT);
    }

    /**
     * 简单的日期格式校验
     *
     * @param date    输入日期，如(yyyy-MM-dd)
     * @param pattern 日期格式
     * @return 有效返回true, 反之false
     */
    public static boolean isValidDate(String date, String pattern) {
        if (StringUtils.isEmpty(date)) {
            return false;
        }

        try {
            toDate(date, pattern);
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
     * @param format 日期格式
     * @return 当前日期字符串
     */
    public static String now(String format) {
        return format(now(), format);
    }

    /**
     * 转换日期字符串为日期对象(默认格式: yyyy-MM-dd HH:mm:ss)
     *
     * @param dateStr 日期字符串
     * @return 日期对象
     */
    public static Date toDate(String dateStr) {
        return toDate(dateStr, DEFAULT_DATE_FORMAT);
    }

    /**
     * 转换日期即字符串为Date对象
     *
     * @param dateStr 日期字符串
     * @param pattern 日期格式
     * @return 日期对象
     */
    public static Date toDate(String dateStr, String pattern) {
        return DateTimeFormat.forPattern(pattern).parseDateTime(dateStr).toDate();
    }

    /**
     * java（毫秒）时间戳
     *
     * @param millis 毫秒
     * @return 日期
     */
    public static Date ofMillis(long millis) {
        return new Date(millis);
    }

    public static Date ofMillis(Long millis) {
        return millis == null ? null : new Date(millis);
    }

    /**
     * unix时间戳
     *
     * @param seconds 秒
     * @return
     */
    public static Date ofSeconds(long seconds) {
        return new Date(seconds * 1000);
    }

    public static Date ofSeconds(Long seconds) {
        return seconds == null ? null : new Date(seconds * 1000);
    }

    /**
     * 格式化日期对象
     *
     * @param date   日期对象
     * @param format 日期格式
     * @return 当前日期字符串
     */
    public static String format(Date date, String format) {
        if (date == null) {
            return null;
        }
        return new DateTime(date).toString(format);
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
        return new DateTime(date).toString(DEFAULT_DATE_FORMAT);
    }

    /**
     * 格式化日期对象
     *
     * @param mills   毫秒
     * @param pattern 格式
     * @return 日期字符串
     */
    public static String format(long mills, String pattern) {
        return new DateTime(mills).toString(pattern);
    }

    // ----------------------------------------------------------------plus

    /**
     * 增加毫秒数
     *
     * @param date        时间
     * @param numOfMillis 毫秒数
     * @return 时间
     */
    public static Date plusMillis(@Nonnull Date date, int numOfMillis) {
        return new DateTime(date).plusMillis(numOfMillis).toDate();
    }

    /**
     * 增加秒数
     *
     * @param date         时间
     * @param numOfSeconds 秒数
     * @return 时间
     */
    public static Date plusSeconds(@Nonnull Date date, int numOfSeconds) {
        return new DateTime(date).plusSeconds(numOfSeconds).toDate();
    }

    /**
     * 增加分钟
     *
     * @param date         时间
     * @param numOfMinutes 分钟数
     * @return 时间
     */
    public static Date plusMinutes(@Nonnull Date date, int numOfMinutes) {
        return new DateTime(date).plusMinutes(numOfMinutes).toDate();
    }

    /**
     * 增加小时
     *
     * @param date       时间
     * @param numOfHours 小时数
     * @return 时间
     */
    public static Date plusHours(@Nonnull Date date, int numOfHours) {
        return new DateTime(date).plusHours(numOfHours).toDate();
    }

    /**
     * 增加天数
     *
     * @param date    时间
     * @param numdays 天数
     * @return 时间
     */
    public static Date plusDays(@Nonnull Date date, int numdays) {
        return new DateTime(date).plusDays(numdays).toDate();
    }

    /**
     * 增加周
     *
     * @param date     时间
     * @param numWeeks 周数
     * @return 时间
     */
    public static Date plusWeeks(@Nonnull Date date, int numWeeks) {
        return new DateTime(date).plusWeeks(numWeeks).toDate();
    }

    /**
     * 增加月份
     *
     * @param date      时间
     * @param numMonths 月数
     * @return 时间
     */
    public static Date plusMonths(@Nonnull Date date, int numMonths) {
        return new DateTime(date).plusMonths(numMonths).toDate();
    }

    /**
     * 增加年
     *
     * @param date     时间
     * @param numYears 年数
     * @return 时间
     */
    public static Date plusYears(@Nonnull Date date, int numYears) {
        return new DateTime(date).plusYears(numYears).toDate();
    }

    // ----------------------------------------------------------------minus

    /**
     * 减少毫秒数
     *
     * @param date        时间
     * @param numOfMillis 毫秒数
     * @return 时间
     */
    public static Date minusMillis(@Nonnull Date date, int numOfMillis) {
        return new DateTime(date).minusMillis(numOfMillis).toDate();
    }

    /**
     * 减少秒数
     *
     * @param date         时间
     * @param numOfSeconds 秒数
     * @return 时间
     */
    public static Date minusSeconds(@Nonnull Date date, int numOfSeconds) {
        return new DateTime(date).minusSeconds(numOfSeconds).toDate();
    }

    /**
     * 减少分钟
     *
     * @param date         时间
     * @param numOfMinutes 分钟数
     * @return 时间
     */
    public static Date minusMinutes(@Nonnull Date date, int numOfMinutes) {
        return new DateTime(date).minusMinutes(numOfMinutes).toDate();
    }

    /**
     * 减少小时
     *
     * @param date       时间
     * @param numOfHours 小时数
     * @return 时间
     */
    public static Date minusHours(@Nonnull Date date, int numOfHours) {
        return new DateTime(date).minusHours(numOfHours).toDate();
    }

    /**
     * 减少天数
     *
     * @param date    时间
     * @param numdays 天数
     * @return 时间
     */
    public static Date minusDays(@Nonnull Date date, int numdays) {
        return new DateTime(date).minusDays(numdays).toDate();
    }

    /**
     * 减少周
     *
     * @param date     时间
     * @param numWeeks 周数
     * @return 时间
     */
    public static Date minusWeeks(@Nonnull Date date, int numWeeks) {
        return new DateTime(date).minusWeeks(numWeeks).toDate();
    }

    /**
     * 减少月份
     *
     * @param date      时间
     * @param numMonths 月数
     * @return 时间
     */
    public static Date minusMonths(@Nonnull Date date, int numMonths) {
        return new DateTime(date).minusMonths(numMonths).toDate();
    }

    /**
     * 减少年
     *
     * @param date     时间
     * @param numYears 年数
     * @return 时间
     */
    public static Date minusYears(@Nonnull Date date, int numYears) {
        return new DateTime(date).minusYears(numYears).toDate();
    }

    // ----------------------------------------------------------------start/end

    /**
     * 获取指定日期所在天的开始时间：yyyy-MM-dd 00:00:00
     *
     * @param date 时间
     * @return 时间
     */
    public static Date startOfDay(@Nonnull Date date) {
        return startOfDay(new DateTime(date));
    }

    /**
     * 获取指定日期所在天的结束时间：yyyy-MM-dd 23:59:59
     *
     * @param date 时间
     * @return 时间
     */
    public static Date endOfDay(@Nonnull Date date) {
        return endOfDay(new DateTime(date));
    }

    /**
     * 获取指定日期所在周的开始时间：yyyy-MM-周一 00:00:00
     *
     * @param date 日期
     * @return 当前周第一天
     */
    public static Date startOfWeek(@Nonnull Date date) {
        return startOfDay(new DateTime(date).dayOfWeek().withMinimumValue());
    }

    /**
     * 获取指定日期所在周的结束时间：yyyy-MM-周日 23:59:59
     *
     * @param date 日期
     * @return 当前周最后一天
     */
    public static Date endOfWeek(@Nonnull Date date) {
        return endOfDay(new DateTime(date).dayOfWeek().withMaximumValue());
    }

    /**
     * 获取指定日期所在月的开始时间：yyyy-MM-01 00:00:00
     *
     * @param date 日期
     * @return 当前月的第一天
     */
    public static Date startOfMonth(@Nonnull Date date) {
        return startOfDay(new DateTime(date).dayOfMonth().withMinimumValue());
    }

    /**
     * 获取指定日期所在月的结束时间：yyyy-MM-月未 23:59:59
     *
     * @param date 日期
     * @return 当前月的最后一天
     */
    public static Date endOfMonth(@Nonnull Date date) {
        return endOfDay(new DateTime(date).dayOfMonth().withMaximumValue());
    }

    /**
     * 获取指定日期所在月的开始时间：yyyy-01-01 00:00:00
     *
     * @param date 日期
     * @return 当前年的第一天
     */
    public static Date startOfYear(@Nonnull Date date) {
        return startOfDay(new DateTime(date).dayOfYear().withMinimumValue());
    }

    /**
     * 获取指定日期所在月的结束时间：yyyy-12-31 23:59:59
     *
     * @param date 日期
     * @return 当前年的最后一天
     */
    public static Date endOfYear(@Nonnull Date date) {
        return endOfDay(new DateTime(date).dayOfYear().withMaximumValue());
    }

    // ----------------------------------------------------------------day of

    /**
     * 获取指定时间所在周的周n，1<=day<=7
     *
     * @param date 相对日期
     * @param day  1:星期一，2:星期二，...
     * @return 本周周几的日期对象
     */
    public static Date withDayOfWeek(@Nonnull Date date, int day) {
        return startOfDay(new DateTime(date).withDayOfWeek(day));
    }

    /**
     * 获取指定时间所在月的n号，1<=day<=31
     *
     * @param date
     * @param day
     * @return
     */
    public static Date withDayOfMonth(@Nonnull Date date, int day) {
        return startOfDay(new DateTime(date).withDayOfMonth(day));
    }

    /**
     * 获取指定时间所在年的n天，1<=day<=366
     *
     * @param date
     * @param day
     * @return
     */
    public static Date withDayOfYear(@Nonnull Date date, int day) {
        return startOfDay(new DateTime(date).withDayOfYear(day));
    }

    // ----------------------------------------------------------------day of
    public static int dayOfYear(@Nonnull Date date) {
        return new DateTime(date).getDayOfYear();
    }

    public static int dayOfMonth(@Nonnull Date date) {
        return new DateTime(date).getDayOfMonth();
    }

    public static int dayOfWeek(@Nonnull Date date) {
        return new DateTime(date).getDayOfWeek();
    }

    public static int hourOfDay(@Nonnull Date date) {
        return new DateTime(date).getHourOfDay();
    }

    // ----------------------------------------------------------------others

    /**
     * 计算两个日期的时间差（单位：秒）
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 时间间隔
     */
    public static long clockDiff(@Nonnull Date start, @Nonnull Date end) {
        return (end.getTime() - start.getTime()) / 1000;
    }

    /**
     * Returns a days between the two date(end-start)
     *
     * @param start the start date
     * @param end   the end date
     * @return a number of between start to end days
     */
    public static int daysBetween(Date start, Date end) {
        return Days.daysBetween(new DateTime(start), new DateTime(end)).getDays();
    }

    /**
     * 日期a是否大于日期b
     *
     * @param source 待比较日期
     * @param target 目标日期
     * @return 大于返回true，反之false
     */
    public static boolean isAfter(@Nonnull Date source, @Nonnull Date target) {
        return source.after(target);
    }

    /**
     * 日期a是否小于日期b
     *
     * @param source 待比较日期
     * @param target 目标日期
     * @return 小于返回true，反之false
     */
    public static boolean isBefore(@Nonnull Date source, @Nonnull Date target) {
        return source.before(target);
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

    public static Date random(long beginMills, long endMills) {
        if (beginMills >= endMills) {
            throw new IllegalArgumentException("Date [" + beginMills + "] must before [" + endMills + "]");
        }

        return new Date(beginMills + ThreadLocalRandom.current().nextLong(endMills - beginMills));
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
        return date.toInstant()
                .atZone(ZoneId.systemDefault()) // .atOffset(ZoneOffset.of("+8"))
                .toLocalDateTime();
    }

    public static Date toDate(LocalDateTime localDateTime) {
        return toDate(localDateTime, ZoneId.systemDefault());
    }

    public static Date toDate(LocalDateTime localDateTime, ZoneId zoneId) {
        return Date.from(localDateTime.atZone(zoneId).toInstant());
    }

    public static LocalDate toLocalDate(LocalDateTime localDateTime) {
        return localDateTime.toLocalDate();
    }

    public static LocalTime toLocalTime(LocalDateTime localDateTime) {
        return localDateTime.toLocalTime();
    }

    public static LocalDateTime toLocalDateTime(LocalDate localDate) {
        return localDate.atStartOfDay();
    }

    public static LocalDateTime toLocalDateTime(LocalDate localDate,
                                                LocalTime localTime) {
        return localDate.atTime(localTime);
    }

    /**
     * Converts date time to cron expression
     *
     * @param date the date
     * @return cron expression of the spec date
     */
    public static String toCronExpression(Date date) {
        LocalDateTime dateTime = Dates.toLocalDateTime(date);
        return new StringBuilder(22)
            .append(dateTime.getSecond()    ).append(BLANK_CHAR) // second
            .append(dateTime.getMinute()    ).append(BLANK_CHAR) // minute
            .append(dateTime.getHour()      ).append(BLANK_CHAR) // hour
            .append(dateTime.getDayOfMonth()).append(BLANK_CHAR) // day
            .append(dateTime.getMonthValue()).append(BLANK_CHAR) // month
            .append('?'                     ).append(BLANK_CHAR) // week
            .append(dateTime.getYear()      )                    // year
            .toString();
    }

    // ----------------------------------------------------------------private methods
    private static Date endOfDay(DateTime date) {
        // 当毫秒数大于499时，存入到Mysql的（datatime）字段数据会自动加1秒，所以此处毫秒为000
        //date.secondOfDay().withMaximumValue().millisOfSecond().withMinimumValue().toDate();
        return date.withTime(23, 59, 59, 0).toDate();
    }

    private static Date startOfDay(DateTime date) {
        return date.withTimeAtStartOfDay().toDate();
    }

}

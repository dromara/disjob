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

import cn.ponfee.disjob.common.util.Jsons;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import static cn.ponfee.disjob.common.date.Dates.*;

/**
 * Dates test
 *
 * @author Ponfee
 */
public class DatesTest {

    @Test
    public void testInstance() {
        String str = "1970-01-01T00:00:00Z";
        Assertions.assertEquals(str, Instant.parse("1970-01-01T00:00:00Z").toString());
        Assertions.assertEquals(str, Instant.parse("1970-01-01T00:00:00.Z").toString());
        Assertions.assertEquals(str, Instant.parse("1970-01-01T00:00:00.0Z").toString());
        Assertions.assertEquals(str, Instant.parse("1970-01-01T00:00:00.00Z").toString());
        Assertions.assertEquals(str, Instant.parse("1970-01-01T00:00:00.000Z").toString());
        Assertions.assertEquals(str, Instant.parse("1970-01-01T00:00:00.0000Z").toString());
        Assertions.assertEquals(str, Instant.parse("1970-01-01T00:00:00.00000Z").toString());
        Assertions.assertEquals(str, Instant.parse("1970-01-01T00:00:00.000000Z").toString());

        System.out.println(Date.from(Instant.now()));              // Instant -> Date
        System.out.println(new Date().toInstant());                // Date    -> Instant
        System.out.println(Instant.parse("1970-01-01T00:00:00Z")); // String  -> Instant

        Assertions.assertEquals(str, Instant.from(DateTimeFormatter.ISO_INSTANT.parse(str)).toString());
        Assertions.assertEquals(str, Instant.ofEpochSecond(0).toString());
        Assertions.assertEquals(str, Instant.ofEpochMilli(0).toString());
        Assertions.assertEquals(str, Instant.ofEpochSecond(0, 0).toString());
        Assertions.assertTrue(Math.abs(new Date().getTime() - Instant.now().toEpochMilli()) <= 1);

        // 输出UTC+0时区的时间点
        System.out.println(Instant.now().toString());
        // 输出本地时区的时间点
        System.out.println(new Date().toString());

        Instant instant = Instant.ofEpochSecond(0, 1234567890L);
        Assertions.assertEquals("1970-01-01T00:00:01.234567890Z", instant.toString());
        Assertions.assertEquals("\"1970-01-01T00:00:01.234567890Z\"", Jsons.toJson(instant));
        Assertions.assertEquals(instant, Jsons.fromJson("\"1970-01-01T00:00:01.234567890Z\"", Instant.class));
    }

    @Test
    public void testLocalDateTime() {
        LocalDateTime date = LocalDateTime.parse("1970-01-01T00:00:00");
        Assertions.assertEquals(date, LocalDateTime.parse("1970-01-01T00:00:00"));
        Assertions.assertEquals(date, LocalDateTime.parse("1970-01-01T00:00:00."));
        Assertions.assertEquals(date, LocalDateTime.parse("1970-01-01T00:00:00.0"));
        Assertions.assertEquals(date, LocalDateTime.parse("1970-01-01T00:00:00.00"));
        Assertions.assertEquals(date, LocalDateTime.parse("1970-01-01T00:00:00.000"));
        Assertions.assertEquals(date, LocalDateTime.parse("1970-01-01T00:00:00.0000"));
        Assertions.assertEquals(date, LocalDateTime.parse("1970-01-01T00:00:00.00000"));
        Assertions.assertEquals(date, LocalDateTime.parse("1970-01-01T00:00:00.000000"));
    }

    @Test
    public void testOffsetDateTime() {
        OffsetDateTime date = OffsetDateTime.parse("1970-01-01T00:00:00+08:00");
        Assertions.assertEquals(date, OffsetDateTime.parse("1970-01-01T00:00:00+08:00"));
        Assertions.assertEquals(date, OffsetDateTime.parse("1970-01-01T00:00:00.+08:00"));
        Assertions.assertEquals(date, OffsetDateTime.parse("1970-01-01T00:00:00.0+08:00"));
        Assertions.assertEquals(date, OffsetDateTime.parse("1970-01-01T00:00:00.00+08:00"));
        Assertions.assertEquals(date, OffsetDateTime.parse("1970-01-01T00:00:00.000+08:00"));
        Assertions.assertEquals(date, OffsetDateTime.parse("1970-01-01T00:00:00.0000+08:00"));
        Assertions.assertEquals(date, OffsetDateTime.parse("1970-01-01T00:00:00.00000+08:00"));
        Assertions.assertEquals(date, OffsetDateTime.parse("1970-01-01T00:00:00.000000+08:00"));

        Assertions.assertThrows(DateTimeParseException.class, () -> OffsetDateTime.parse("1970-01-01T00:00:00.00+08"));
        Assertions.assertEquals("1970-01-01T08:00+08:00[Asia/Shanghai]", ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault()).toString());
    }

    @Test
    public void testDateTimeFormat() throws ParseException {
        Date date = new Date(0);
        Assertions.assertEquals("1970-01-01T08:00:00.000+08", FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSX").format(date));
        Assertions.assertEquals("1970-01-01T08:00:00.000+08:00", FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(date));
        Assertions.assertEquals("1970-01-01 08:00:00", Dates.format(FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSX").parse("1970-01-01T08:00:00.000+08")));
        Assertions.assertEquals("1970-01-01 08:00:00", Dates.format(FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").parse("1970-01-01T08:00:00.000+08:00")));
        Assertions.assertEquals("1970-01-01 08:00:00", Dates.format(FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSX").parse("1970-01-01T08:00:00.000+08:00")));
        //Assertions.assertEquals("1970-01-01 08:00:00", Dates.format(FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").parse("1970-01-01T08:00:00.000+08")));

        System.out.println(DateTimeFormatter.BASIC_ISO_DATE.format(LocalDateTime.now()));
        System.out.println(DateTimeFormatter.ISO_ZONED_DATE_TIME.format(OffsetDateTime.now()));
        System.out.println(LocalDate.from(DateTimeFormatter.BASIC_ISO_DATE.parse("20250816")));

        System.out.println(Instant.parse("2000-01-01T00:00:00.000Z").toString());
        System.out.println(OffsetDateTime.parse("2000-01-01T00:00:00.000+08:00").toString());
        System.out.println(LocalDateTime.parse("2000-01-01T00:00:00.000").toString());
    }

    @Test
    public void test() throws ParseException {
        Date now = new Date(0);
        System.out.println(now); // Thu Jan 01 08:00:00 CST 1970
        System.out.println(new Date("Thu Jan 01 00:00:00 CST 1970")); // Thu Jan 01 14:00:00 CST 1970
        Assertions.assertEquals(now, toDate(LocalDateTime.parse(now.toString(), DateTimeFormatter.ofPattern(DATE_TO_STRING_PATTERN, Locale.ROOT))));

        System.out.println(FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss.SSS").format(System.currentTimeMillis()));
        String str = "2023-01-03 15:23:45.321";
        Date date = parse(str, DATETIME_MILLI_PATTERN);
        Assertions.assertNotNull(JavaUtilDateFormat.DEFAULT.parse(str));
        Assertions.assertNull(JavaUtilDateFormat.DEFAULT.parse(null));
        Assertions.assertTrue(isValidDate(str, DATETIME_PATTERN));
        Assertions.assertFalse(isValidDate(null, DATETIME_PATTERN));
        Assertions.assertFalse(isValidDate("2020-xx-00 00:00:00", DATETIME_PATTERN));

        Assertions.assertEquals("0002-11-30 00:00:00.000", format(new Date(-62170185600000L), DATETIME_MILLI_PATTERN));

        Assertions.assertEquals("1970-01-01 08:00:01", format(new Date(1000)));
        Assertions.assertEquals("1970-01-01 08:00:00", format(new Date(1)));
        Assertions.assertEquals(str, format(JavaUtilDateFormat.DEFAULT.parse(str), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals(str, format(date, DATETIME_MILLI_PATTERN));

        Assertions.assertEquals("2023-01-03 15:23:45.731", format(plusMillis(date, 410), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2023-01-03 15:23:48.321", format(plusSeconds(date, 3), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2023-01-03 15:26:45.321", format(plusMinutes(date, 3), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2023-01-03 18:23:45.321", format(plusHours(date, 3), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2023-01-06 15:23:45.321", format(plusDays(date, 3), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2023-01-24 15:23:45.321", format(plusWeeks(date, 3), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2023-04-03 15:23:45.321", format(plusMonths(date, 3), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2026-01-03 15:23:45.321", format(plusYears(date, 3), DATETIME_MILLI_PATTERN));

        Assertions.assertEquals("2023-01-03 15:23:45.210", format(minusMillis(date, 111), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2023-01-03 15:23:42.321", format(minusSeconds(date, 3), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2023-01-03 15:20:45.321", format(minusMinutes(date, 3), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2023-01-03 12:23:45.321", format(minusHours(date, 3), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2022-12-31 15:23:45.321", format(minusDays(date, 3), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2022-12-13 15:23:45.321", format(minusWeeks(date, 3), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2022-10-03 15:23:45.321", format(minusMonths(date, 3), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2020-01-03 15:23:45.321", format(minusYears(date, 3), DATETIME_MILLI_PATTERN));

        Assertions.assertEquals("2023-01-03 00:00:00.000", format(startOfDay(date), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2023-01-03 23:59:59.999", format(endOfDay(date), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2023-01-02 00:00:00.000", format(startOfWeek(date), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2023-01-08 23:59:59.999", format(endOfWeek(date), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2023-01-01 00:00:00.000", format(startOfMonth(date), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2023-01-31 23:59:59.999", format(endOfMonth(date), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2023-01-01 00:00:00.000", format(startOfYear(date), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2023-12-31 23:59:59.999", format(endOfYear(date), DATETIME_MILLI_PATTERN));

        Assertions.assertEquals("2023-01-06 15:23:45.321", format(withDayOfWeek(date, 5), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2023-01-02 15:23:45.321", format(withDayOfWeek(date, 1), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2023-01-03 15:23:45.321", format(withDayOfWeek(date, 2), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2023-01-04 15:23:45.321", format(withDayOfWeek(date, 3), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2023-01-05 15:23:45.321", format(withDayOfMonth(date, 5), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2023-06-05 15:23:45.321", format(withDayOfMonth(parse("2023-06-25 15:23:45.321", DATETIME_MILLI_PATTERN), 5), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2023-01-05 15:23:45.321", format(withDayOfYear(date, 5), DATETIME_MILLI_PATTERN));
        Assertions.assertEquals("2023-01-05 15:23:45.321", format(withDayOfYear(parse("2023-06-25 15:23:45.321", DATETIME_MILLI_PATTERN), 5), DATETIME_MILLI_PATTERN));

        Assertions.assertEquals(15, hourOfDay(date));
        Assertions.assertEquals(2, dayOfWeek(date));
        Assertions.assertEquals(3, dayOfMonth(date));
        Assertions.assertEquals(156, dayOfYear(parse("2023-06-05 15:23:45.321", DATETIME_MILLI_PATTERN)));

        Assertions.assertEquals(15, daysBetween(JavaUtilDateFormat.DEFAULT.parse("2023-05-21 15:23:45"), JavaUtilDateFormat.DEFAULT.parse("2023-06-05 15:23:45")));

        Assertions.assertEquals("45 23 15 3 1 ? 2023", toCronExpression(JavaUtilDateFormat.DEFAULT.parse(str)));
    }

    @Test
    public void test2() throws ParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        LocalDateTime localDateTime = LocalDateTime.parse("2023-05-21T15:23:45.123");
        Assertions.assertEquals("2023-05-21 00:00:00.000", formatter.format(Dates.startOfDay(localDateTime)));
        Assertions.assertEquals("2023-05-21 23:59:59.999", formatter.format(Dates.endOfDay(localDateTime)));
        Assertions.assertEquals("2023-05-21 15:23:45.123", DATETIME_MILLI_FORMAT.format(Dates.toDate(localDateTime)));
        Assertions.assertEquals("2023-05-21 00:00:00.000", DATETIME_MILLI_FORMAT.format(Dates.toDate(localDateTime.toLocalDate())));
        Assertions.assertEquals("2023-05-21T15:23:45.123", Dates.toLocalDateTime(Dates.toDate(localDateTime)).toString());
        Assertions.assertEquals("2023-05-21", Dates.toLocalDate(Dates.toDate(localDateTime)).toString());

        Assertions.assertEquals("2023-05-21T07:23:45.123", Dates.toUTC(localDateTime).toString());
        Assertions.assertEquals("2023-05-21T23:23:45.123", Dates.fromUTC(localDateTime).toString());
        Assertions.assertEquals("2023-05-21T16:23:45.123", Dates.convertZone(localDateTime, ZoneOffset.of("+09:00")).toString());

        Instant javaInstantCreatedAtWrite = Instant.ofEpochSecond(0, 123456000);
        // java instant write to database datetime
        LocalDateTime dbDatetimeCreatedAt = javaInstantCreatedAtWrite.atZone(ZoneOffset.UTC).toLocalDateTime();
        // database datetime read to java instant
        Instant javaInstantCreatedAtRead = dbDatetimeCreatedAt.atZone(ZoneOffset.UTC).toInstant();
        // assertions
        Assertions.assertEquals("1970-01-01T00:00:00.123456Z", javaInstantCreatedAtWrite.toString());
        Assertions.assertEquals("1970-01-01T00:00:00.123456", dbDatetimeCreatedAt.toString());
        Assertions.assertEquals("1970-01-01T00:00:00.123456Z", javaInstantCreatedAtRead.toString());
    }

    @Test
    public void testDateFormat() {
        DateEntity entity = Jsons.fromJson("{\"createTime\":\"2000-03-01 00:00:00\"}", DateEntity.class);
        Assertions.assertEquals("2000-03-01 00:00:00", Dates.format(entity.getCreateTime()));
    }

    @Test
    public void testDateMax() throws ParseException {
        Date a = JavaUtilDateFormat.DEFAULT.parse("2021-10-12 12:34:23");
        Date b = JavaUtilDateFormat.DEFAULT.parse("2021-10-12 12:34:24");

        Assertions.assertEquals(null, Dates.max(null, null));
        Assertions.assertEquals(null, Dates.min(null, null));

        Assertions.assertEquals(a, Dates.max(a, null));
        Assertions.assertEquals(a, Dates.max(null, a));

        Assertions.assertEquals(a, Dates.min(a, null));
        Assertions.assertEquals(a, Dates.min(null, a));

        Assertions.assertEquals(b, Dates.max(a, b));
        Assertions.assertEquals(b, Dates.max(b, a));

        Assertions.assertEquals(a, Dates.min(a, b));
        Assertions.assertEquals(a, Dates.min(b, a));
    }

    @Test
    public void testPeriods() {
        String format = "yyyy-MM-dd HH:mm:ss.SSS";
        Date origin = Dates.parse("2017-10-21 12:23:32.000", format);
        Date target = Dates.parse("2018-10-21 11:54:12.000", format);
        int step = 3;
        DatePeriods periods = DatePeriods.HOURLY;
        DatePeriods.Segment segment = periods.next(origin, target, step, 0);
        Assertions.assertEquals("2018-10-21 09:23:32.000 ~ 2018-10-21 12:23:31.999", segment.toString());

        segment = periods.next(segment.begin(), segment.begin(), step, 0);
        Assertions.assertEquals("2018-10-21 09:23:32.000 ~ 2018-10-21 12:23:31.999", segment.toString());

        segment = periods.next(segment.begin(), step, 0);
        Assertions.assertEquals("2018-10-21 09:23:32.000 ~ 2018-10-21 12:23:31.999", segment.toString());

        segment = periods.next(segment.begin(), step, 1);
        Assertions.assertEquals("2018-10-21 12:23:32.000 ~ 2018-10-21 15:23:31.999", segment.toString());

        segment = periods.next(segment.begin(), step, 1);
        Assertions.assertEquals("2018-10-21 15:23:32.000 ~ 2018-10-21 18:23:31.999", segment.toString());

        // SECOND
        periods = DatePeriods.PER_SECOND;
        origin = Dates.parse("2022-05-21 12:23:12.000", format);
        target = Dates.parse("2022-05-21 12:23:23.000", format);
        step = 5;
        segment = periods.next(origin, target, step, 0);
        Assertions.assertEquals("2022-05-21 12:23:22.000 ~ 2022-05-21 12:23:26.999", segment.toString());

        segment = periods.next(segment.begin(), step, 1);
        Assertions.assertEquals("2022-05-21 12:23:27.000 ~ 2022-05-21 12:23:31.999", segment.toString());

        segment = periods.next(segment.begin(), step, 1);
        Assertions.assertEquals("2022-05-21 12:23:32.000 ~ 2022-05-21 12:23:36.999", segment.toString());

        // QUARTERLY
        periods = DatePeriods.QUARTERLY;
        origin = Dates.parse("2021-08-21 12:23:12.000", format);
        target = Dates.parse("2022-05-21 23:34:23.000", format);
        step = 2;
        segment = periods.next(origin, target, step, 0);
        Assertions.assertEquals("2022-02-21 12:23:12.000 ~ 2022-08-21 12:23:11.999", segment.toString());

        segment = periods.next(segment.begin(), step, 1);
        Assertions.assertEquals("2022-08-21 12:23:12.000 ~ 2023-02-21 12:23:11.999", segment.toString());

        segment = periods.next(segment.begin(), step, 1);
        Assertions.assertEquals("2023-02-21 12:23:12.000 ~ 2023-08-21 12:23:11.999", segment.toString());
    }

    @Test
    public void testDuration() {
        Assertions.assertEquals("PT5M", Duration.ofSeconds(300).toString());
        Assertions.assertEquals(3600, Duration.parse("PT1H").getSeconds());
    }

    @Test
    public void testDateString() throws ParseException {
        Assertions.assertEquals(21, Dates.toCronExpression(LocalDateTime.of(2000, 12, 31, 23, 59, 59, 99999999)).length());
        Date date = new Date();
        System.out.println("Dates.format(date): " + Dates.format(date));
        String dateString = date.toString();
        System.out.println("date.toString(): " + dateString);
        Date parsed1 = FastDateFormat.getInstance(DATE_TO_STRING_PATTERN, Locale.ENGLISH).parse(dateString);
        System.out.println(Dates.format(parsed1));
        Assertions.assertNotEquals(date, parsed1);

        System.out.println("--------");
        FastDateFormat format = FastDateFormat.getInstance(DATE_TO_STRING_PATTERN, Locale.CHINA);
        dateString = format.format(date);
        System.out.println("FastDateFormat.format(date): " + dateString);
        Date parsed2 = format.parse(dateString);
        System.out.println(Dates.format(parsed2));
        Assertions.assertNotEquals(date, parsed2);
    }

    @Test
    public void testStreamMax() throws ParseException {
        Date max = Arrays.stream(new Date[]{JavaUtilDateFormat.DEFAULT.parse("2020-01-03 00:00:00"), JavaUtilDateFormat.DEFAULT.parse("2020-01-02 00:00:00")})
            .max(Comparator.naturalOrder())
            .orElse(null);
        System.out.println(Dates.format(max));
    }

    @Test
    public void testZone() throws ParseException {
        String source = "Wed Jul 19 21:14:25 CST 2023";
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TO_STRING_PATTERN, Locale.ROOT);

        // 14小时时差
        Assertions.assertEquals("2023-07-20 11:14:25", Dates.format(FastDateFormat.getInstance(DATE_TO_STRING_PATTERN, Locale.ENGLISH).parse(source)));
        Assertions.assertEquals("2023-07-20 11:14:25", Dates.format(new Date(source)));
        Assertions.assertEquals("2023-07-20 10:14:25", Dates.format(Date.from(ZonedDateTime.parse(source, dateTimeFormatter).toInstant())));

        // 正常
        Assertions.assertEquals("2023-07-19 21:14:25", Dates.format(Dates.toDate(LocalDateTime.parse(source, dateTimeFormatter))));
    }

    @Test
    public void testFormatDuration() {
        Assertions.assertEquals("0.000s", formatDuration(0));
        Assertions.assertEquals("0.001s", formatDuration(1));
        Assertions.assertEquals("0.999s", formatDuration(999));
        Assertions.assertEquals("1.000s", formatDuration(1_000));
        Assertions.assertEquals("9.999s", formatDuration(10_000 - 1));
        Assertions.assertEquals("10.00s", formatDuration(10_000));
        Assertions.assertEquals("100.0s", formatDuration(100_000));
        Assertions.assertEquals("999.9s", formatDuration(1_000_000 - 1));

        Assertions.assertEquals("16.66m", formatDuration(1_000_000));
        Assertions.assertEquals("16.66m", formatDuration(1_000_001));
        Assertions.assertEquals("100.0m", formatDuration(6_000_000));
        Assertions.assertEquals("150.0m", formatDuration(9_000_001));
        Assertions.assertEquals("333.3m", formatDuration(20_000_001));
        Assertions.assertEquals("666.6m", formatDuration(40_000_001));
        Assertions.assertEquals("833.3m", formatDuration(50_000_001));
        Assertions.assertEquals("999.9m", formatDuration(60_000_000 - 1));

        Assertions.assertEquals("16.66h", formatDuration(60_000_000));
        Assertions.assertEquals("16.66h", formatDuration(60_000_001));
        Assertions.assertEquals("25.00h", formatDuration(90_000_001));
        Assertions.assertEquals("100.0h", formatDuration(360_000_000));
        Assertions.assertEquals("277.7h", formatDuration(999_999_999));
        Assertions.assertEquals("444.4h", formatDuration(1_599_999_999));
        Assertions.assertEquals("722.2h", formatDuration(2_599_999_999L));
        Assertions.assertEquals("999.9h", formatDuration(3_600_000_000L - 1));

        Assertions.assertEquals("41.66d", formatDuration(3_600_000_000L));
        Assertions.assertEquals("41.66d", formatDuration(3_600_000_001L));
        Assertions.assertEquals("64.81d", formatDuration(5_600_000_000L));
        Assertions.assertEquals("100.0d", formatDuration(8_640_000_000L));
        Assertions.assertEquals("999.9d", formatDuration(86_400_000_000L - 1));

        Assertions.assertEquals("1000d", formatDuration(86_400_000_000L));
        Assertions.assertEquals("10000d", formatDuration(864_000_000_000L));
        Assertions.assertEquals("100000d", formatDuration(8_640_000_000_000L));

        Assertions.assertEquals("00:00:00.000", DurationFormatUtils.formatDurationHMS(0L));
        Assertions.assertEquals("01:40:00.000", DurationFormatUtils.formatDurationHMS(6_000_000L));
        Assertions.assertEquals("25:00:00.000", DurationFormatUtils.formatDurationHMS(90_000_000L));
        Assertions.assertEquals("99:59:59.999", DurationFormatUtils.formatDurationHMS(360_000_000L - 1));
        Assertions.assertEquals("100:00:00.000", DurationFormatUtils.formatDurationHMS(360_000_000L));
        Assertions.assertEquals("1000:00:00.000", DurationFormatUtils.formatDurationHMS(3_600_000_000L));
        Assertions.assertEquals("P0Y0M41DT16H0M0.000S", DurationFormatUtils.formatDurationISO(3_600_000_000L));
    }

    @Setter
    @Getter
    public static class DateEntity {
        @JsonProperty(value = "createTime")
        @JsonFormat(pattern = Dates.DATETIME_PATTERN)
        private Date createTime;
    }

}

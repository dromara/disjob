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
import org.apache.commons.lang3.time.FastDateFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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

    static int round = 1_000;

    @Test
    public void test() throws ParseException {
        String str = "2023-01-03 15:23:45.321";
        Date date = toDate(str, DATEFULL_PATTERN);
        Assertions.assertNotNull(JavaUtilDateFormat.DEFAULT.parse(str));
        Assertions.assertNull(JavaUtilDateFormat.DEFAULT.parse(null));
        Assertions.assertTrue(isValidDate(str, DATETIME_PATTERN));
        Assertions.assertFalse(isValidDate(null, DATETIME_PATTERN));
        Assertions.assertFalse(isValidDate("2020-xx-00 00:00:00", DATETIME_PATTERN));

        Assertions.assertEquals("0002-11-30 00:00:00.000", format(ofTimeMillis(-62170185600000L), DATEFULL_PATTERN));

        Assertions.assertEquals("1970-01-01 08:00:01", format(ofUnixTimestamp(1)));
        Assertions.assertEquals("1970-01-01 08:00:00", format(ofTimeMillis(1)));
        Assertions.assertEquals(19, now(DATETIME_PATTERN).length());
        Assertions.assertEquals(str, format(JavaUtilDateFormat.DEFAULT.parse(str), DATEFULL_PATTERN));
        Assertions.assertEquals(str, format(date, DATEFULL_PATTERN));
        Assertions.assertEquals("1970-01-01 08:00:02.312", format(2312, DATEFULL_PATTERN));

        Assertions.assertEquals("2023-01-03 15:23:45.731", format(plusMillis(date, 410), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-03 15:23:48.321", format(plusSeconds(date, 3), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-03 15:26:45.321", format(plusMinutes(date, 3), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-03 18:23:45.321", format(plusHours(date, 3), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-06 15:23:45.321", format(plusDays(date, 3), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-24 15:23:45.321", format(plusWeeks(date, 3), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-04-03 15:23:45.321", format(plusMonths(date, 3), DATEFULL_PATTERN));
        Assertions.assertEquals("2026-01-03 15:23:45.321", format(plusYears(date, 3), DATEFULL_PATTERN));

        Assertions.assertEquals("2023-01-03 15:23:45.210", format(minusMillis(date, 111), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-03 15:23:42.321", format(minusSeconds(date, 3), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-03 15:20:45.321", format(minusMinutes(date, 3), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-03 12:23:45.321", format(minusHours(date, 3), DATEFULL_PATTERN));
        Assertions.assertEquals("2022-12-31 15:23:45.321", format(minusDays(date, 3), DATEFULL_PATTERN));
        Assertions.assertEquals("2022-12-13 15:23:45.321", format(minusWeeks(date, 3), DATEFULL_PATTERN));
        Assertions.assertEquals("2022-10-03 15:23:45.321", format(minusMonths(date, 3), DATEFULL_PATTERN));
        Assertions.assertEquals("2020-01-03 15:23:45.321", format(minusYears(date, 3), DATEFULL_PATTERN));

        Assertions.assertEquals("2023-01-03 00:00:00.000", format(startOfDay(date), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-03 23:59:59.000", format(endOfDay(date), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-02 00:00:00.000", format(startOfWeek(date), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-08 23:59:59.000", format(endOfWeek(date), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-01 00:00:00.000", format(startOfMonth(date), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-31 23:59:59.000", format(endOfMonth(date), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-01 00:00:00.000", format(startOfYear(date), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-12-31 23:59:59.000", format(endOfYear(date), DATEFULL_PATTERN));

        Assertions.assertEquals("2023-01-06 15:23:45.321", format(withDayOfWeek(date, 5), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-02 15:23:45.321", format(withDayOfWeek(date, 1), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-03 15:23:45.321", format(withDayOfWeek(date, 2), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-04 15:23:45.321", format(withDayOfWeek(date, 3), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-05 15:23:45.321", format(withDayOfMonth(date, 5), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-06-05 15:23:45.321", format(withDayOfMonth(toDate("2023-06-25 15:23:45.321", DATEFULL_PATTERN), 5), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-05 15:23:45.321", format(withDayOfYear(date, 5), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-05 15:23:45.321", format(withDayOfYear(toDate("2023-06-25 15:23:45.321", DATEFULL_PATTERN), 5), DATEFULL_PATTERN));

        Assertions.assertEquals(15, hourOfDay(date));
        Assertions.assertEquals(2, dayOfWeek(date));
        Assertions.assertEquals(3, dayOfMonth(date));
        Assertions.assertEquals(156, dayOfYear(toDate("2023-06-05 15:23:45.321", DATEFULL_PATTERN)));

        Assertions.assertEquals(15, daysBetween(JavaUtilDateFormat.DEFAULT.parse("2023-05-21 15:23:45"),JavaUtilDateFormat.DEFAULT.parse("2023-06-05 15:23:45")));

        Assertions.assertEquals("45 23 15 3 1 ? 2023", toCronExpression(JavaUtilDateFormat.DEFAULT.parse(str)));
    }

    @Test
    public void testSimpleDateFormat() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date date = new Date();
        for (int i = 0; i < round; i++) {
            format.format(date);
        }
    }

    @Test
    public void testFastDateFormat() {
        FastDateFormat format = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss.SSS");
        Date date = new Date();
        for (int i = 0; i < round; i++) {
            format.format(date);
        }
    }

    @Test
    public void testDateFormat() {
       DateEntity entity = Jsons.fromJson("{\"createTime\":\"2000-03-01 00:00:00\"}",DateEntity.class);
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
        Date origin = Dates.toDate("2017-10-21 12:23:32.000", format);
        Date target = Dates.toDate("2018-10-21 11:54:12.000", format);
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
        origin = Dates.toDate("2022-05-21 12:23:12.000", format);
        target = Dates.toDate("2022-05-21 12:23:23.000", format);
        step = 5;
        segment = periods.next(origin, target, step, 0);
        Assertions.assertEquals("2022-05-21 12:23:22.000 ~ 2022-05-21 12:23:26.999", segment.toString());

        segment = periods.next(segment.begin(), step, 1);
        Assertions.assertEquals("2022-05-21 12:23:27.000 ~ 2022-05-21 12:23:31.999", segment.toString());

        segment = periods.next(segment.begin(), step, 1);
        Assertions.assertEquals("2022-05-21 12:23:32.000 ~ 2022-05-21 12:23:36.999", segment.toString());

        // QUARTERLY
        periods = DatePeriods.QUARTERLY;
        origin = Dates.toDate("2021-08-21 12:23:12.000", format);
        target = Dates.toDate("2022-05-21 23:34:23.000", format);
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

    @Setter
    @Getter
    public static class DateEntity {
        @JsonProperty(value = "createTime")
        @JsonFormat(pattern = Dates.DATETIME_PATTERN)
        private Date createTime;
    }

}

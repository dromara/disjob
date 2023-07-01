/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.date;

import cn.ponfee.disjob.common.util.Jsons;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
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

    public static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance(Dates.DATETIME_PATTERN);

    static int round = 1_000_000;

    @Test
    public void test() {
        String str = "2023-01-03 15:23:45.321";
        Assertions.assertTrue(isValidDate(str));
        Assertions.assertTrue(isValidDate(str, DATETIME_PATTERN));
        Assertions.assertFalse(isValidDate("2020-xx-00 00:00:00", DATETIME_PATTERN));

        Assertions.assertTrue(isZeroDate(toDate(ZERO_DATE_TIME)));
        Assertions.assertTrue(isZeroDate(toDate(ZERO_DATE_TIME, DATETIME_PATTERN)));
        Assertions.assertEquals(19, now(DATETIME_PATTERN).length());
        Assertions.assertEquals(str, format(toDate(str), DATEFULL_PATTERN));
        Assertions.assertEquals(str, format(toDate(str, DATEFULL_PATTERN), DATEFULL_PATTERN));
        Assertions.assertEquals("1970-01-01 08:00:02.312", format(2312, DATEFULL_PATTERN));

        Assertions.assertEquals("2023-01-03 15:23:45.731", format(plusMillis(toDate(str, DATEFULL_PATTERN), 410), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-03 15:23:48.321", format(plusSeconds(toDate(str, DATEFULL_PATTERN), 3), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-03 15:26:45.321", format(plusMinutes(toDate(str, DATEFULL_PATTERN), 3), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-03 18:23:45.321", format(plusHours(toDate(str, DATEFULL_PATTERN), 3), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-06 15:23:45.321", format(plusDays(toDate(str, DATEFULL_PATTERN), 3), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-24 15:23:45.321", format(plusWeeks(toDate(str, DATEFULL_PATTERN), 3), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-04-03 15:23:45.321", format(plusMonths(toDate(str, DATEFULL_PATTERN), 3), DATEFULL_PATTERN));
        Assertions.assertEquals("2026-01-03 15:23:45.321", format(plusYears(toDate(str, DATEFULL_PATTERN), 3), DATEFULL_PATTERN));

        Assertions.assertEquals("2023-01-03 15:23:45.210", format(minusMillis(toDate(str, DATEFULL_PATTERN), 111), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-03 15:23:42.321", format(minusSeconds(toDate(str, DATEFULL_PATTERN), 3), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-03 15:20:45.321", format(minusMinutes(toDate(str, DATEFULL_PATTERN), 3), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-03 12:23:45.321", format(minusHours(toDate(str, DATEFULL_PATTERN), 3), DATEFULL_PATTERN));
        Assertions.assertEquals("2022-12-31 15:23:45.321", format(minusDays(toDate(str, DATEFULL_PATTERN), 3), DATEFULL_PATTERN));
        Assertions.assertEquals("2022-12-13 15:23:45.321", format(minusWeeks(toDate(str, DATEFULL_PATTERN), 3), DATEFULL_PATTERN));
        Assertions.assertEquals("2022-10-03 15:23:45.321", format(minusMonths(toDate(str, DATEFULL_PATTERN), 3), DATEFULL_PATTERN));
        Assertions.assertEquals("2020-01-03 15:23:45.321", format(minusYears(toDate(str, DATEFULL_PATTERN), 3), DATEFULL_PATTERN));

        Assertions.assertEquals("2023-01-03 00:00:00.000", format(startOfDay(toDate(str, DATEFULL_PATTERN)), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-03 23:59:59.000", format(endOfDay(toDate(str, DATEFULL_PATTERN)), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-02 00:00:00.000", format(startOfWeek(toDate(str, DATEFULL_PATTERN)), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-08 23:59:59.000", format(endOfWeek(toDate(str, DATEFULL_PATTERN)), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-01 00:00:00.000", format(startOfMonth(toDate(str, DATEFULL_PATTERN)), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-31 23:59:59.000", format(endOfMonth(toDate(str, DATEFULL_PATTERN)), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-01 00:00:00.000", format(startOfYear(toDate(str, DATEFULL_PATTERN)), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-12-31 23:59:59.000", format(endOfYear(toDate(str, DATEFULL_PATTERN)), DATEFULL_PATTERN));

        Assertions.assertEquals("2023-01-06 15:23:45.321", format(withDayOfWeek(toDate(str, DATEFULL_PATTERN), 5), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-02 15:23:45.321", format(withDayOfWeek(toDate(str, DATEFULL_PATTERN), 1), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-03 15:23:45.321", format(withDayOfWeek(toDate(str, DATEFULL_PATTERN), 2), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-04 15:23:45.321", format(withDayOfWeek(toDate(str, DATEFULL_PATTERN), 3), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-05 15:23:45.321", format(withDayOfMonth(toDate(str, DATEFULL_PATTERN), 5), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-06-05 15:23:45.321", format(withDayOfMonth(toDate("2023-06-25 15:23:45.321", DATEFULL_PATTERN), 5), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-05 15:23:45.321", format(withDayOfYear(toDate(str, DATEFULL_PATTERN), 5), DATEFULL_PATTERN));
        Assertions.assertEquals("2023-01-05 15:23:45.321", format(withDayOfYear(toDate("2023-06-25 15:23:45.321", DATEFULL_PATTERN), 5), DATEFULL_PATTERN));

        Assertions.assertEquals(15, hourOfDay(toDate(str, DATEFULL_PATTERN)));
        Assertions.assertEquals(2, dayOfWeek(toDate(str, DATEFULL_PATTERN)));
        Assertions.assertEquals(3, dayOfMonth(toDate(str, DATEFULL_PATTERN)));
        Assertions.assertEquals(156, dayOfYear(toDate("2023-06-05 15:23:45.321", DATEFULL_PATTERN)));

        Assertions.assertEquals(15, daysBetween(toDate("2023-05-21 15:23:45"),toDate("2023-06-05 15:23:45")));

        Assertions.assertEquals("45 23 15 3 1 ? 2023", toCronExpression(toDate(str)));
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
    public void testDateFormat() throws ParseException {
        String json = "{\"createTime\":\"" + Dates.ZERO_DATE_TIME + "\"}";
       DateEntity dateEntity = Jsons.fromJson(json,DateEntity.class);


       DateEntity entity = Jsons.fromJson("{\"createTime\":\"2000-03-01 00:00:00\"}",DateEntity.class);
        Assertions.assertEquals("2000-03-01 00:00:00", Dates.format(entity.getCreateTime()));

        // test format
        Assertions.assertEquals("0002-11-30 00:00:00", DateFormatUtils.format(dateEntity.getCreateTime(), Dates.DATETIME_PATTERN));
        Assertions.assertEquals("0002-11-30 00:00:00", DATE_FORMAT.format(dateEntity.getCreateTime()));
        //Assertions.assertEquals("-0001-11-28 00:05:43", Dates.format(orderDriverEntity.getCreateTime(), Dates.DEFAULT_DATETIME_FORMAT)); error

        long time = -62170185600000L;
        Assertions.assertEquals(time, new Date(time).getTime());
        Assertions.assertTrue(Dates.isZeroDate(new Date(time)));

        // test parse
        Date zeroDate = new Date(time);
        Assertions.assertEquals(zeroDate, dateEntity.getCreateTime());
        Assertions.assertEquals(zeroDate, new Date(zeroDate.getTime()));
        Assertions.assertEquals(zeroDate, DateUtils.parseDate(Dates.ZERO_DATE_TIME, Dates.DATETIME_PATTERN));
        Assertions.assertEquals(zeroDate, DATE_FORMAT.parse(Dates.ZERO_DATE_TIME));
        //Assertions.assertEquals(time, Dates.toDate(zeroDateStr, Dates.DEFAULT_DATETIME_FORMAT).getTime()); error
    }

    @Test
    public void testDateMax() {
        Date a = Dates.toDate("2021-10-12 12:34:23");
        Date b = Dates.toDate("2021-10-12 12:34:24");

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
        Date parsed1 = FastDateFormat.getInstance("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH).parse(dateString);
        System.out.println(Dates.format(parsed1));
        Assertions.assertNotEquals(date, parsed1);

        System.out.println("--------");
        FastDateFormat format = FastDateFormat.getInstance("EEE MMM dd HH:mm:ss zzz yyyy", Locale.CHINA);
        dateString = format.format(date);
        System.out.println("FastDateFormat.format(date): " + dateString);
        Date parsed2 = format.parse(dateString);
        System.out.println(Dates.format(parsed2));
        Assertions.assertNotEquals(date, parsed2);
    }

    @Test
    public void testStreamMax() {
        Date max = Arrays.stream(new Date[]{Dates.toDate("2020-01-03 00:00:00"), Dates.toDate("2020-01-02 00:00:00")})
            .max(Comparator.naturalOrder())
            .orElse(null);
        System.out.println(Dates.format(max));
    }

    @Data
    public static class DateEntity {
        @JsonProperty(value = "createTime")
        @JsonFormat(pattern = Dates.DATETIME_PATTERN)
        private Date createTime;
    }

}

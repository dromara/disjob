package cn.ponfee.scheduler.supervisor.test.common.util;

import cn.ponfee.scheduler.common.date.DatePeriods;
import cn.ponfee.scheduler.common.date.Dates;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.joda.time.format.DateTimeFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/**
 * @author Ponfee
 */
public class DateTest {

    public static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance(Dates.DEFAULT_DATE_FORMAT);

    static int round = 1_000_000;

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
        String zeroDateStr = "0000-00-00 00:00:00";
        String json = "{\"createTime\":\"" + zeroDateStr + "\"}";
        DateEntity dateEntity = JSON.parseObject(json, DateEntity.class);


        DateEntity entity = JSON.parseObject("{\"createTime\":\"2000-15-01 00:00:00\"}", DateEntity.class);
        Assertions.assertEquals("2001-03-01 00:00:00", Dates.format(entity.getCreateTime()));

        // test format
        Assertions.assertEquals("0002-11-30 00:00:00", DateFormatUtils.format(dateEntity.getCreateTime(), Dates.DEFAULT_DATE_FORMAT));
        Assertions.assertEquals("0002-11-30 00:00:00", DATE_FORMAT.format(dateEntity.getCreateTime()));
        //Assertions.assertEquals("-0001-11-28 00:05:43", Dates.format(orderDriverEntity.getCreateTime(), Dates.DEFAULT_DATE_FORMAT)); error

        long time = -62170185600000L;
        Assertions.assertEquals(time, new Date(time).getTime());
        Assertions.assertTrue(Dates.isZeroDate(new Date(time)));

        // test parse
        Date zeroDate = new Date(time);
        Assertions.assertEquals(zeroDate, dateEntity.getCreateTime());
        Assertions.assertEquals(zeroDate, new Date(zeroDate.getTime()));
        Assertions.assertEquals(zeroDate, DateUtils.parseDate(zeroDateStr, Dates.DEFAULT_DATE_FORMAT));
        Assertions.assertEquals(zeroDate, DATE_FORMAT.parse(zeroDateStr));
        //Assertions.assertEquals(time, Dates.toDate(zeroDateStr, Dates.DEFAULT_DATE_FORMAT).getTime()); error
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
    public void testDateTimeFormat() {
        Assertions.assertThrows(NullPointerException.class, () -> DateTimeFormat.forPattern(Dates.DEFAULT_DATE_FORMAT).parseDateTime(null).toDate());
        Assertions.assertThrows(IllegalArgumentException.class, () -> DateTimeFormat.forPattern(Dates.DEFAULT_DATE_FORMAT).parseDateTime("null").toDate());
        Assertions.assertThrows(IllegalArgumentException.class, () -> DateTimeFormat.forPattern(Dates.DEFAULT_DATE_FORMAT).parseDateTime("").toDate());
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
        @JSONField(name = "createTime", format = Dates.DEFAULT_DATE_FORMAT)
        private Date createTime;
    }

}

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

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

import static cn.ponfee.disjob.common.date.Dates.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * LocalDateTimeFormatTest
 *
 * @author Ponfee
 */
public class LocalDateTimeFormatTest {

    private static final DateTimeFormatter COMPACT_DATETIME_FORMATTER = DateTimeFormatter.ofPattern(Dates.DATETIME_COMPACT_PATTERN);

    private static final DateTimeFormatter PATTERN_11 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter PATTERN_12 = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private static final DateTimeFormatter PATTERN_13 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter PATTERN_14 = DateTimeFormatter.ofPattern("yyyy/MM/dd'T'HH:mm:ss");

    private static final DateTimeFormatter PATTERN_21 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter PATTERN_22 = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS");
    private static final DateTimeFormatter PATTERN_23 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private static final DateTimeFormatter PATTERN_24 = DateTimeFormatter.ofPattern("yyyy/MM/dd'T'HH:mm:ss.SSS");

    @Test
    public void test9() throws ParseException {
        LocalDateTime localDateTime = LocalDateTime.of(2022, 01, 02, 03, 04, 05, 123000000);
        Date date = toDate(localDateTime);

        assertEquals("2022-01-02T03:04:05.123", localDateTime.toString());
        assertEquals("2022-01-02 03:04:05", format(date));
        assertEquals("2022-01-02T07:04:05.123", convertZone(localDateTime, ZoneId.of("UTC+4"), ZoneId.of("UTC+8")).toString());

        LocalDateTime originLocalDateTime = LocalDateTime.of(1970, 01, 01, 00, 00, 00, 0);
        Date originDate = toDate(originLocalDateTime); // defaultZone: UTC+8
        assertEquals("1970-01-01T08:00", convertZone(originLocalDateTime, ZoneId.of("UTC+0"), ZoneId.of("UTC+8")).toString());
        assertEquals("1969-12-31T16:00", convertZone(originLocalDateTime, ZoneId.of("UTC+8"), ZoneId.of("UTC+0")).toString());

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATETIME_PATTERN);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone(ZoneId.of("UTC+0")));
        assertEquals("1969-12-31 16:00:00", simpleDateFormat.format(originDate));

        assertEquals("1970-01-01 00:00:00", PATTERN_11.format(LocalDateTimeFormat.DEFAULT.parse("Thu Jan 01 00:00:00 CST 1970")));
        assertEquals("2000-01-01 00:00:00", PATTERN_11.format(LocalDateTimeFormat.DEFAULT.parse("2000-01-01T00:00:00.000+08:00")));
    }

    @Test
    public void test8() {
        LocalDateTime localDateTime = LocalDateTime.now();
        Date date = toDate(localDateTime);

        System.out.println(localDateTime);
        System.out.println(format(date));

        System.out.println("\n-------toDate");
        System.out.println(toDate(localDateTime));
        System.out.println(toDate(localDateTime));

        System.out.println("\n-------toLocalDateTime");
        System.out.println(toLocalDateTime(date));
        System.out.println(toLocalDateTime(date));

        System.out.println("\n-------zoneConvert1");
        LocalDateTime targetLocalDateTime = convertZone(localDateTime, ZoneId.of("UTC+8"), ZoneId.of("UTC+0"));
        System.out.println(targetLocalDateTime);

        System.out.println("\n-------zoneConvert2");
        targetLocalDateTime = convertZone(localDateTime, ZoneId.of("UTC+0"), ZoneId.of("UTC+8"));
        System.out.println(targetLocalDateTime);
    }

    @Test
    public void test0() {
        LocalDateTime localDateTime = LocalDateTime.of(2022, 01, 02, 03, 05, 05, 123000000);
        assertEquals("2022-01-02T03:05:05.123", localDateTime.toString());

        assertEquals("20220102030505", COMPACT_DATETIME_FORMATTER.format(localDateTime));
        assertEquals("2022-01-02 03:05:05", PATTERN_11.format(localDateTime));
        assertEquals("2022/01/02 03:05:05", PATTERN_12.format(localDateTime));
        assertEquals("2022-01-02T03:05:05", PATTERN_13.format(localDateTime));
        assertEquals("2022/01/02T03:05:05", PATTERN_14.format(localDateTime));
        assertEquals("2022-01-02 03:05:05.123", PATTERN_21.format(localDateTime));
        assertEquals("2022/01/02 03:05:05.123", PATTERN_22.format(localDateTime));
        assertEquals("2022-01-02T03:05:05.123", PATTERN_23.format(localDateTime));
        assertEquals("2022/01/02T03:05:05.123", PATTERN_24.format(localDateTime));

        assertEquals("{},ISO resolved to 2022-01-02T03:05:05", COMPACT_DATETIME_FORMATTER.parse("20220102030505").toString());
        assertEquals("{},ISO resolved to 2022-01-02T03:05:05", PATTERN_11.parse("2022-01-02 03:05:05").toString());
        assertEquals("{},ISO resolved to 2022-01-02T03:05:05", PATTERN_12.parse("2022/01/02 03:05:05").toString());
        assertEquals("{},ISO resolved to 2022-01-02T03:05:05", PATTERN_13.parse("2022-01-02T03:05:05").toString());
        assertEquals("{},ISO resolved to 2022-01-02T03:05:05", PATTERN_14.parse("2022/01/02T03:05:05").toString());
        assertEquals("{},ISO resolved to 2022-01-02T03:05:05.123", PATTERN_21.parse("2022-01-02 03:05:05.123").toString());
        assertEquals("{},ISO resolved to 2022-01-02T03:05:05.123", PATTERN_22.parse("2022/01/02 03:05:05.123").toString());
        assertEquals("{},ISO resolved to 2022-01-02T03:05:05.123", PATTERN_23.parse("2022-01-02T03:05:05.123").toString());
        assertEquals("{},ISO resolved to 2022-01-02T03:05:05.123", PATTERN_24.parse("2022/01/02T03:05:05.123").toString());


        assertEquals("2022-01-02T03:05:05", LocalDateTimeFormat.DEFAULT.parse("20220102030505").toString());
        assertEquals("2022-01-02T03:05:05", LocalDateTimeFormat.DEFAULT.parse("2022-01-02 03:05:05").toString());
        assertEquals("2022-01-02T03:05:05", LocalDateTimeFormat.DEFAULT.parse("2022/01/02 03:05:05").toString());
        assertEquals("2022-01-02T03:05:05", LocalDateTimeFormat.DEFAULT.parse("2022-01-02T03:05:05").toString());
        assertEquals("2022-01-02T03:05:05", LocalDateTimeFormat.DEFAULT.parse("2022/01/02T03:05:05").toString());
        assertEquals("2022-01-02T03:05:05.123", LocalDateTimeFormat.DEFAULT.parse("2022-01-02 03:05:05.123").toString());
        assertEquals("2022-01-02T03:05:05.123", LocalDateTimeFormat.DEFAULT.parse("2022/01/02 03:05:05.123").toString());
        assertEquals("2022-01-02T03:05:05.123", LocalDateTimeFormat.DEFAULT.parse("2022-01-02T03:05:05.123").toString());
        assertEquals("2022-01-02T03:05:05.123", LocalDateTimeFormat.DEFAULT.parse("2022/01/02T03:05:05.123").toString());


        assertEquals("2022-01-02T11:05:05.123", LocalDateTimeFormat.DEFAULT.parse("2022-01-02T03:05:05.123Z").toString());
        assertEquals("2022-01-02T11:05:05.123", LocalDateTimeFormat.DEFAULT.parse("2022/01/02T03:05:05.123Z").toString());

        Date date = toDate(localDateTime);
        assertEquals("Sun Jan 02 03:05:05 CST 2022", date.toString());
        assertEquals("2022-01-02T03:05:05.123", LocalDateTimeFormat.DEFAULT.parse(String.valueOf(date.getTime())).toString());
        assertEquals("2022-01-02T03:05:05", LocalDateTimeFormat.DEFAULT.parse(String.valueOf(date.getTime() / 1000)).toString());
    }

    @Test
    public void test1() {
        String dateString = "2022-07-19T13:44:27.873";
        LocalDateTime date = LocalDateTime.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        LocalDateTimeFormat format = LocalDateTimeFormat.DEFAULT;
        assertEquals(dateString, date.toString());

        assertEquals("20220719134427", COMPACT_DATETIME_FORMATTER.format(date));
        assertEquals("2022-07-19 13:44:27", PATTERN_11.format(date));
        assertEquals("2022/07/19 13:44:27", PATTERN_12.format(date));
        assertEquals("2022-07-19T13:44:27", PATTERN_13.format(date));
        assertEquals("2022/07/19T13:44:27", PATTERN_14.format(date));
        assertEquals("2022-07-19 13:44:27.873", PATTERN_21.format(date));
        assertEquals("2022/07/19 13:44:27.873", PATTERN_22.format(date));
        assertEquals("2022-07-19T13:44:27.873", PATTERN_23.format(date));
        assertEquals("2022/07/19T13:44:27.873", PATTERN_24.format(date));

        assertEquals("{},ISO resolved to 2022-07-19T13:44:27", COMPACT_DATETIME_FORMATTER.parse(COMPACT_DATETIME_FORMATTER.format(date)).toString());
        assertEquals("{},ISO resolved to 2022-07-19T13:44:27", PATTERN_11.parse(PATTERN_11.format(date)).toString());
        assertEquals("{},ISO resolved to 2022-07-19T13:44:27", PATTERN_12.parse(PATTERN_12.format(date)).toString());
        assertEquals("{},ISO resolved to 2022-07-19T13:44:27", PATTERN_13.parse(PATTERN_13.format(date)).toString());
        assertEquals("{},ISO resolved to 2022-07-19T13:44:27", PATTERN_14.parse(PATTERN_14.format(date)).toString());
        assertEquals("{},ISO resolved to 2022-07-19T13:44:27.873", PATTERN_21.parse(PATTERN_21.format(date)).toString());
        assertEquals("{},ISO resolved to 2022-07-19T13:44:27.873", PATTERN_22.parse(PATTERN_22.format(date)).toString());
        assertEquals("{},ISO resolved to 2022-07-19T13:44:27.873", PATTERN_23.parse(PATTERN_23.format(date)).toString());
        assertEquals("{},ISO resolved to 2022-07-19T13:44:27.873", PATTERN_24.parse(PATTERN_24.format(date)).toString());

        assertEquals("2022-07-19T13:44:27", format.parse(COMPACT_DATETIME_FORMATTER.format(date)).toString());
        assertEquals("2022-07-19T13:44:27", format.parse(PATTERN_11.format(date)).toString());
        assertEquals("2022-07-19T13:44:27", format.parse(PATTERN_12.format(date)).toString());
        assertEquals("2022-07-19T13:44:27", format.parse(PATTERN_13.format(date)).toString());
        assertEquals("2022-07-19T13:44:27", format.parse(PATTERN_14.format(date)).toString());
        assertEquals("2022-07-19T13:44:27.873", format.parse(PATTERN_21.format(date)).toString());
        assertEquals("2022-07-19T13:44:27.873", format.parse(PATTERN_22.format(date)).toString());
        assertEquals("2022-07-19T13:44:27.873", format.parse(PATTERN_23.format(date)).toString());
        assertEquals("2022-07-19T13:44:27.873", format.parse(PATTERN_24.format(date)).toString());
    }

    @Test
    public void test2() throws ParseException {
        LocalDateTimeFormat format = LocalDateTimeFormat.DEFAULT;
        assertEquals("2022-07-18T00:00", format.parse("20220718").toString());
        assertEquals("2022-07-18T00:00", format.parse("2022-07-18").toString());
        assertEquals("2022-07-18T00:00", format.parse("2022/07/18").toString());

        assertEquals("2022-07-18T15:45:59", format.parse("20220718154559").toString());
        assertEquals("2022-07-18T15:45:59", format.parse("2022-07-18 15:45:59").toString());
        assertEquals("2022-07-18T15:45:59", format.parse("2022/07/18 15:45:59").toString());

        assertEquals("2022-07-18T15:45:59", format.parse("2022-07-18T15:45:59").toString());
        assertEquals("2022-07-18T15:45:59", format.parse("2022/07/18T15:45:59").toString());

        assertEquals("2022-07-18T15:45:59.414", format.parse("2022-07-18 15:45:59.414").toString());
        assertEquals("2022-07-18T15:45:59.414", format.parse("2022/07/18 15:45:59.414").toString());

        assertEquals("2022-07-18T15:45:59.414", format.parse("2022-07-18T15:45:59.414").toString());
        assertEquals("2022-07-18T15:45:59.414", format.parse("2022/07/18T15:45:59.414").toString());

        assertEquals("2022-07-18T15:45:59", format.parse("1658130359").toString());
        assertEquals("2022-07-18T15:45:59", format.parse("1658130359000").toString());
        assertEquals("2001-09-10T21:59:19", format.parse("1000130359").toString());
        assertEquals("2001-09-10T21:59:19", format.parse("1000130359000").toString());

        assertEquals("2022-07-18T23:11:11", format.parse("2022-07-18T15:11:11Z").toString());
        assertEquals("2022-07-18T23:11:11", format.parse("2022-07-18T15:11:11.Z").toString());
        assertEquals("2022-07-18T23:11:11.100", format.parse("2022-07-18T15:11:11.1Z").toString());
        assertEquals("2022-07-18T23:11:11.130", format.parse("2022-07-18T15:11:11.13Z").toString());
        assertEquals("2022-07-18T23:11:11.133", format.parse("2022-07-18T15:11:11.133Z").toString());

        assertEquals("2022-07-18T23:11:11", format.parse("2022/07/18T15:11:11Z").toString());
        assertEquals("2022-07-18T23:11:11", format.parse("2022/07/18T15:11:11.Z").toString());
        assertEquals("2022-07-18T23:11:11.100", format.parse("2022/07/18T15:11:11.1Z").toString());
        assertEquals("2022-07-18T23:11:11.130", format.parse("2022/07/18T15:11:11.13Z").toString());
        assertEquals("2022-07-18T23:11:11.133", format.parse("2022/07/18T15:11:11.133Z").toString());

        assertEquals("2022-07-18T23:11:11.133", format.parse("2022/07/18 15:11:11.133Z").toString());
        assertEquals("2022-07-18T23:11:11.133", format.parse("2022-07-18 15:11:11.133Z").toString());

        assertThrows(Exception.class, () -> format.parse("2022-07-18T1:1:1Z"));

    }

    @Test
    public void test3() {
        String string1 = "2022-07-18T15:11:11.133";
        assertEquals("2022-07-18T15:11:11.133", LocalDateTime.parse(string1, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toString());
        String string2 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.parse(string1, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        assertEquals(string1, string2);

        String dateString = "2022-07-18T15:11:11.133Z";
        LocalDateTime date = LocalDateTime.ofInstant(Instant.parse(dateString), ZoneOffset.UTC);
        assertEquals(date.toString(), "2022-07-18T15:11:11.133");

        date = LocalDateTime.ofInstant(Instant.parse(dateString), ZoneOffset.ofHours(8));
        assertEquals(date.toString(), "2022-07-18T23:11:11.133");
        System.out.println(LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));

        System.out.println(DateTimeFormatter.ISO_INSTANT.parse("2000-01-01T01:23:45.123456789Z"));
        System.out.println(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        System.out.println(DateTimeFormatter.ISO_DATE_TIME.format(OffsetDateTime.now()));
        System.out.println(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()));
        System.out.println(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now()));
    }

    @Test
    public void test4() throws ParseException {
        String text = "2022/07/18T15:11:11.133Z";
        System.out.println(JavaUtilDateFormat.DEFAULT.parse(text));
        System.out.println(LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy/MM/dd'T'HH:mm:ss.SSS'Z'")));

        /*
        DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
            .appendLiteral('T')
            .append(DateTimeFormatter.ISO_LOCAL_TIME)
            .appendInstant()
            .toFormatter();
        */

        LocalDateTimeFormat format = LocalDateTimeFormat.DEFAULT;
        /*System.out.println(format.parse("2022/07/18T15:11:11.1Z").toString());
        System.out.println(format.parse("2022/07/18T15:11:11Z").toString());
        System.out.println(format.parse("2022/07/18T15:11:11.13Z").toString());
        System.out.println(format.parse("2022/07/18T15:11:11.133Z").toString());*/
    }
}

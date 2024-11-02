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

import org.apache.commons.lang3.time.FastDateFormat;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JavaUtilDateFormatTest
 *
 * @author Ponfee
 */
public class JavaUtilDateFormatTest {

    @Test
    public void test0() throws ParseException {
        Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse("2022-01-02 03:04:05.678");
        assertEquals("Sun Jan 02 03:04:05 CST 2022", date.toString());
        assertEquals("Sun Jan 02 17:04:05 CST 2022", new Date(date.toString()).toString());
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(Dates.DATE_TO_STRING_PATTERN, Locale.ROOT);
        assertEquals("Sun Jan 02 03:04:05 CST 2022", Dates.toDate(LocalDateTime.parse(date.toString(), dtf)).toString());
        assertEquals("Sun Jan 02 17:04:05 CST 2022", Date.from(ZonedDateTime.parse(date.toString(), dtf).toInstant()).toString());

        assertEquals("202201", JavaUtilDateFormat.PATTERN_11.format(date));
        assertEquals("2022-01", JavaUtilDateFormat.PATTERN_12.format(date));
        assertEquals("2022/01", JavaUtilDateFormat.PATTERN_13.format(date));
        assertEquals("20220102", JavaUtilDateFormat.PATTERN_21.format(date));
        assertEquals("2022-01-02", JavaUtilDateFormat.PATTERN_22.format(date));
        assertEquals("2022/01/02", JavaUtilDateFormat.PATTERN_23.format(date));
        assertEquals("20220102030405", JavaUtilDateFormat.PATTERN_31.format(date));
        assertEquals("20220102030405678", JavaUtilDateFormat.PATTERN_32.format(date));
        assertEquals("2022-01-02 03:04:05", JavaUtilDateFormat.PATTERN_41.format(date));
        assertEquals("2022/01/02 03:04:05", JavaUtilDateFormat.PATTERN_42.format(date));
        assertEquals("2022-01-02T03:04:05", JavaUtilDateFormat.PATTERN_43.format(date));
        assertEquals("2022/01/02T03:04:05", JavaUtilDateFormat.PATTERN_44.format(date));
        assertEquals("2022-01-02 03:04:05.678", JavaUtilDateFormat.PATTERN_51.format(date));
        assertEquals("2022/01/02 03:04:05.678", JavaUtilDateFormat.PATTERN_52.format(date));
        assertEquals("2022-01-02T03:04:05.678", JavaUtilDateFormat.PATTERN_53.format(date));
        assertEquals("2022/01/02T03:04:05.678", JavaUtilDateFormat.PATTERN_54.format(date));
        assertEquals("2022-01-02 03:04:05.678Z", JavaUtilDateFormat.PATTERN_61.format(date));
        assertEquals("2022/01/02 03:04:05.678Z", JavaUtilDateFormat.PATTERN_62.format(date));
        assertEquals("2022-01-02T03:04:05.678Z", JavaUtilDateFormat.PATTERN_63.format(date));
        assertEquals("2022/01/02T03:04:05.678Z", JavaUtilDateFormat.PATTERN_64.format(date));
        assertEquals("2022-01-02T03:04:05.678+08", JavaUtilDateFormat.PATTERN_73.format(date));
        assertEquals("2022/01/02T03:04:05.678+08", JavaUtilDateFormat.PATTERN_74.format(date));
        assertEquals("2022-01-02 03:04:05", JavaUtilDateFormat.DEFAULT.format(date));


        assertEquals("Sat Jan 01 00:00:00 CST 2022", JavaUtilDateFormat.PATTERN_11.parse("202201").toString());
        assertEquals("Sat Jan 01 00:00:00 CST 2022", JavaUtilDateFormat.PATTERN_12.parse("2022-01").toString());
        assertEquals("Sat Jan 01 00:00:00 CST 2022", JavaUtilDateFormat.PATTERN_13.parse("2022/01").toString());
        assertEquals("Sun Jan 02 00:00:00 CST 2022", JavaUtilDateFormat.PATTERN_21.parse("20220102").toString());
        assertEquals("Sun Jan 02 00:00:00 CST 2022", JavaUtilDateFormat.PATTERN_22.parse("2022-01-02").toString());
        assertEquals("Sun Jan 02 00:00:00 CST 2022", JavaUtilDateFormat.PATTERN_23.parse("2022/01/02").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.PATTERN_31.parse("20220102030405").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.PATTERN_32.parse("20220102030405678").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.PATTERN_41.parse("2022-01-02 03:04:05").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.PATTERN_42.parse("2022/01/02 03:04:05").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.PATTERN_43.parse("2022-01-02T03:04:05").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.PATTERN_44.parse("2022/01/02T03:04:05").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.PATTERN_51.parse("2022-01-02 03:04:05.678").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.PATTERN_52.parse("2022/01/02 03:04:05.678").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.PATTERN_53.parse("2022-01-02T03:04:05.678").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.PATTERN_54.parse("2022/01/02T03:04:05.678").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.PATTERN_61.parse("2022-01-02 03:04:05.678Z").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.PATTERN_62.parse("2022/01/02 03:04:05.678Z").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.PATTERN_63.parse("2022-01-02T03:04:05.678Z").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.PATTERN_64.parse("2022/01/02T03:04:05.678Z").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.PATTERN_73.parse("2022-01-02T03:04:05.678+08").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.PATTERN_74.parse("2022/01/02T03:04:05.678+08").toString());

        assertEquals("Sat Jan 01 00:00:00 CST 2022", JavaUtilDateFormat.DEFAULT.parse("202201").toString());
        assertEquals("Sat Jan 01 00:00:00 CST 2022", JavaUtilDateFormat.DEFAULT.parse("2022-01").toString());
        assertEquals("Sat Jan 01 00:00:00 CST 2022", JavaUtilDateFormat.DEFAULT.parse("2022/01").toString());
        assertEquals("Sun Jan 02 00:00:00 CST 2022", JavaUtilDateFormat.DEFAULT.parse("20220102").toString());
        assertEquals("Sun Jan 02 00:00:00 CST 2022", JavaUtilDateFormat.DEFAULT.parse("2022-01-02").toString());
        assertEquals("Sun Jan 02 00:00:00 CST 2022", JavaUtilDateFormat.DEFAULT.parse("2022/01/02").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.DEFAULT.parse("20220102030405").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.DEFAULT.parse("20220102030405678").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.DEFAULT.parse("2022-01-02 03:04:05").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.DEFAULT.parse("2022/01/02 03:04:05").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.DEFAULT.parse("2022-01-02T03:04:05").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.DEFAULT.parse("2022/01/02T03:04:05").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.DEFAULT.parse("2022-01-02 03:04:05.678").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.DEFAULT.parse("2022/01/02 03:04:05.678").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.DEFAULT.parse("2022-01-02T03:04:05.678").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.DEFAULT.parse("2022/01/02T03:04:05.678").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.DEFAULT.parse("2022-01-02 03:04:05.678Z").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.DEFAULT.parse("2022/01/02 03:04:05.678Z").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.DEFAULT.parse("2022-01-02T03:04:05.678Z").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.DEFAULT.parse("2022/01/02T03:04:05.678Z").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.DEFAULT.parse("2022-01-02T03:04:05.678+08").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.DEFAULT.parse("2022/01/02T03:04:05.678+08").toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.DEFAULT.parse("Sun Jan 02 03:04:05 CST 2022").toString()); // FIXME


        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.DEFAULT.parse(String.valueOf(date.getTime())).toString());
        assertEquals("Sun Jan 02 03:04:05 CST 2022", JavaUtilDateFormat.DEFAULT.parse(String.valueOf(date.getTime() / 1000)).toString());
    }

    @Test
    public void test1() throws ParseException {
        String dateString = "2022-07-19T13:44:27.873Z";
        Date date = JavaUtilDateFormat.PATTERN_73.parse(dateString);

        assertEquals("202207", JavaUtilDateFormat.PATTERN_11.format(date));
        assertEquals("2022-07", JavaUtilDateFormat.PATTERN_12.format(date));
        assertEquals("2022/07", JavaUtilDateFormat.PATTERN_13.format(date));

        assertEquals("20220719", JavaUtilDateFormat.PATTERN_21.format(date));
        assertEquals("2022-07-19", JavaUtilDateFormat.PATTERN_22.format(date));
        assertEquals("2022/07/19", JavaUtilDateFormat.PATTERN_23.format(date));

        assertEquals("20220719214427", JavaUtilDateFormat.PATTERN_31.format(date));
        assertEquals("20220719214427873", JavaUtilDateFormat.PATTERN_32.format(date));

        assertEquals("2022-07-19 21:44:27", JavaUtilDateFormat.PATTERN_41.format(date));
        assertEquals("2022/07/19 21:44:27", JavaUtilDateFormat.PATTERN_42.format(date));
        assertEquals("2022-07-19T21:44:27", JavaUtilDateFormat.PATTERN_43.format(date));
        assertEquals("2022/07/19T21:44:27", JavaUtilDateFormat.PATTERN_44.format(date));

        assertEquals("2022-07-19 21:44:27.873", JavaUtilDateFormat.PATTERN_51.format(date));
        assertEquals("2022/07/19 21:44:27.873", JavaUtilDateFormat.PATTERN_52.format(date));
        assertEquals("2022-07-19T21:44:27.873", JavaUtilDateFormat.PATTERN_53.format(date));
        assertEquals("2022/07/19T21:44:27.873", JavaUtilDateFormat.PATTERN_54.format(date));

        assertEquals("2022-07-19 21:44:27.873Z", JavaUtilDateFormat.PATTERN_61.format(date));
        assertEquals("2022/07/19 21:44:27.873Z", JavaUtilDateFormat.PATTERN_62.format(date));
        assertEquals("2022-07-19T21:44:27.873Z", JavaUtilDateFormat.PATTERN_63.format(date));
        assertEquals("2022/07/19T21:44:27.873Z", JavaUtilDateFormat.PATTERN_64.format(date));

        assertEquals("2022-07-19T21:44:27.873+08", JavaUtilDateFormat.PATTERN_73.format(date));
        assertEquals("2022/07/19T21:44:27.873+08", JavaUtilDateFormat.PATTERN_74.format(date));
    }

    @Test
    public void test2() throws ParseException {
        JavaUtilDateFormat format = JavaUtilDateFormat.DEFAULT;
        FastDateFormat fastDateFormat = JavaUtilDateFormat.PATTERN_51;

        assertEquals("2022-07-01 00:00:00.000", fastDateFormat.format(format.parse("202207")));
        assertEquals("2022-07-01 00:00:00.000", fastDateFormat.format(format.parse("2022-07")));
        assertEquals("2022-07-01 00:00:00.000", fastDateFormat.format(format.parse("2022/07")));

        assertEquals("2022-07-19 00:00:00.000", fastDateFormat.format(format.parse("20220719")));
        assertEquals("2022-07-19 00:00:00.000", fastDateFormat.format(format.parse("2022-07-19")));
        assertEquals("2022-07-19 00:00:00.000", fastDateFormat.format(format.parse("2022/07/19")));

        assertEquals("2022-07-19 21:44:27.000", fastDateFormat.format(format.parse("20220719214427")));
        assertEquals("2022-07-19 21:44:27.873", fastDateFormat.format(format.parse("20220719214427873")));

        assertEquals("2022-07-19 21:44:27.000", fastDateFormat.format(format.parse("2022-07-19 21:44:27")));
        assertEquals("2022-07-19 21:44:27.000", fastDateFormat.format(format.parse("2022/07/19 21:44:27")));
        assertEquals("2022-07-19 21:44:27.000", fastDateFormat.format(format.parse("2022-07-19T21:44:27")));
        assertEquals("2022-07-19 21:44:27.000", fastDateFormat.format(format.parse("2022/07/19T21:44:27")));

        assertEquals("2022-07-19 21:44:27.873", fastDateFormat.format(format.parse("2022-07-19 21:44:27.873")));
        assertEquals("2022-07-19 21:44:27.873", fastDateFormat.format(format.parse("2022/07/19 21:44:27.873")));
        assertEquals("2022-07-19 21:44:27.873", fastDateFormat.format(format.parse("2022-07-19T21:44:27.873")));
        assertEquals("2022-07-19 21:44:27.873", fastDateFormat.format(format.parse("2022/07/19T21:44:27.873")));

        assertEquals("2022-07-19 21:44:27.000", fastDateFormat.format(format.parse("2022-07-19T21:44:27Z")));
        assertEquals("2022-07-19 21:44:27.000", fastDateFormat.format(format.parse("2022-07-19T21:44:27.Z")));
        assertEquals("2022-07-19 21:44:27.800", fastDateFormat.format(format.parse("2022-07-19T21:44:27.8Z")));
        assertEquals("2022-07-19 21:44:27.870", fastDateFormat.format(format.parse("2022-07-19T21:44:27.87Z")));
        assertEquals("2022-07-19 21:44:27.873", fastDateFormat.format(format.parse("2022-07-19T21:44:27.873Z")));

        assertEquals("2022-07-19 21:44:27.000", fastDateFormat.format(format.parse("2022/07/19T21:44:27Z")));
        assertEquals("2022-07-19 21:44:27.000", fastDateFormat.format(format.parse("2022/07/19T21:44:27.Z")));
        assertEquals("2022-07-19 21:44:27.300", fastDateFormat.format(format.parse("2022/07/19T21:44:27.3Z")));
        assertEquals("2022-07-19 21:44:27.730", fastDateFormat.format(format.parse("2022/07/19T21:44:27.73Z")));
        assertEquals("2022-07-19 21:44:27.873", fastDateFormat.format(format.parse("2022/07/19T21:44:27.873Z")));


        assertEquals("2022-07-19 21:44:27.000", fastDateFormat.format(format.parse("2022-07-19 21:44:27Z")));
        assertEquals("2022-07-19 21:44:27.000", fastDateFormat.format(format.parse("2022-07-19 21:44:27.Z")));
        assertEquals("2022-07-19 21:44:27.800", fastDateFormat.format(format.parse("2022-07-19 21:44:27.8Z")));
        assertEquals("2022-07-19 21:44:27.870", fastDateFormat.format(format.parse("2022-07-19 21:44:27.87Z")));
        assertEquals("2022-07-19 21:44:27.873", fastDateFormat.format(format.parse("2022-07-19 21:44:27.873Z")));

        assertEquals("2022-07-19 21:44:27.000", fastDateFormat.format(format.parse("2022/07/19 21:44:27Z")));
        assertEquals("2022-07-19 21:44:27.000", fastDateFormat.format(format.parse("2022/07/19 21:44:27.Z")));
        assertEquals("2022-07-19 21:44:27.300", fastDateFormat.format(format.parse("2022/07/19 21:44:27.3Z")));
        assertEquals("2022-07-19 21:44:27.730", fastDateFormat.format(format.parse("2022/07/19 21:44:27.73Z")));
        assertEquals("2022-07-19 21:44:27.873", fastDateFormat.format(format.parse("2022/07/19 21:44:27.873Z")));


        assertEquals("2022-07-19 21:44:27.873", fastDateFormat.format(format.parse("2022-07-19T21:44:27.873+08")));
        assertEquals("2022-07-19 21:44:27.873", fastDateFormat.format(format.parse("2022/07/19T21:44:27.873+08")));

        assertEquals("2022-07-19 21:44:27.000", fastDateFormat.format(format.parse("Tue Jul 19 21:44:27 CST 2022")));

        String dateString = "2022-07-19T13:44:27.873Z";
        Date date = JavaUtilDateFormat.PATTERN_73.parse(dateString);
        assertEquals("2022-07-19 21:44:27.000", fastDateFormat.format(format.parse(Long.toString(date.getTime() / 1000))));
        assertEquals("2022-07-19 21:44:27.873", fastDateFormat.format(format.parse(Long.toString(date.getTime()))));
    }

    @Test
    public void test() throws ParseException {
        JavaUtilDateFormat format = JavaUtilDateFormat.DEFAULT;
        Date date = new Date();

        System.out.println(format.parse(JavaUtilDateFormat.PATTERN_11.format(date)));
        System.out.println(format.parse(JavaUtilDateFormat.PATTERN_12.format(date)));
        System.out.println(format.parse(JavaUtilDateFormat.PATTERN_21.format(date)));
        System.out.println(format.parse(JavaUtilDateFormat.PATTERN_22.format(date)));
        System.out.println(format.parse(JavaUtilDateFormat.PATTERN_31.format(date)));
        System.out.println(format.parse(JavaUtilDateFormat.PATTERN_41.format(date)));
        System.out.println(format.parse(JavaUtilDateFormat.PATTERN_32.format(date)));
        System.out.println(format.parse(JavaUtilDateFormat.PATTERN_51.format(date)));
        System.out.println(format.parse(JavaUtilDateFormat.PATTERN_63.format(date)));
        System.out.println(format.parse(JavaUtilDateFormat.PATTERN_13.format(date)));
        System.out.println(format.parse(JavaUtilDateFormat.PATTERN_23.format(date)));
        System.out.println(format.parse(JavaUtilDateFormat.PATTERN_42.format(date)));
        System.out.println(format.parse(JavaUtilDateFormat.PATTERN_52.format(date)));
        System.out.println(format.parse(JavaUtilDateFormat.PATTERN_64.format(date)));
        System.out.println(format.parse(String.valueOf(date.getTime())));
        System.out.println(format.parse(String.valueOf(date.getTime() / 1000)));

        System.out.println("\n------------------------");
        System.out.println(JavaUtilDateFormat.PATTERN_11.format(date));
        System.out.println(JavaUtilDateFormat.PATTERN_12.format(date));
        System.out.println(JavaUtilDateFormat.PATTERN_21.format(date));
        System.out.println(JavaUtilDateFormat.PATTERN_22.format(date));
        System.out.println(JavaUtilDateFormat.PATTERN_31.format(date));
        System.out.println(JavaUtilDateFormat.PATTERN_41.format(date));
        System.out.println(JavaUtilDateFormat.PATTERN_32.format(date));
        System.out.println(JavaUtilDateFormat.PATTERN_51.format(date));
        System.out.println(JavaUtilDateFormat.PATTERN_63.format(date));
        System.out.println(JavaUtilDateFormat.PATTERN_13.format(date));
        System.out.println(JavaUtilDateFormat.PATTERN_23.format(date));
        System.out.println(JavaUtilDateFormat.PATTERN_42.format(date));
        System.out.println(JavaUtilDateFormat.PATTERN_52.format(date));
        System.out.println(JavaUtilDateFormat.PATTERN_64.format(date));

        System.out.println("\n------------------------");
        assertTrue(JavaUtilDateFormat.TOSTRING_PATTERN.matcher("Sat Jun 01 22:36:21 CST 2019").matches());
        assertFalse(JavaUtilDateFormat.TOSTRING_PATTERN.matcher("Sat Jun 0122:36:21 CST 2019").matches());
        assertFalse(JavaUtilDateFormat.TOSTRING_PATTERN.matcher("sat Jun 01 22:36:21 CST 2019").matches());
        assertFalse(JavaUtilDateFormat.TOSTRING_PATTERN.matcher("Sat Jun 01 22:36:21 DST 2019").matches());
        assertFalse(JavaUtilDateFormat.TOSTRING_PATTERN.matcher("Sat jun 01 22:36:21 CST 2019").matches());
        assertFalse(JavaUtilDateFormat.TOSTRING_PATTERN.matcher("Sat Jun 01 22:36:21CST 2019").matches());
        System.out.println(JavaUtilDateFormat.DEFAULT.parse("Sat Jun 01 22:36:21 CST 2019", new ParsePosition(0)));
        System.out.println(format.parse("Sat Jun 01 22:36:21 CST 2019"));
        System.out.println(format.parse("2020-12-01 10:33:06"));
        System.out.println(format.parse("1644894528086"));
        System.out.println(format.parse("1644894528"));
        System.out.println(new JavaUtilDateFormat("yyyy").parse("2022"));
        System.out.println(JavaUtilDateFormat.DEFAULT.format(new Date(0)));

        assertTrue(JavaUtilDateFormat.TIMESTAMP_PATTERN.matcher("0").matches());
        assertTrue(JavaUtilDateFormat.TIMESTAMP_PATTERN.matcher("1").matches());
        assertTrue(JavaUtilDateFormat.TIMESTAMP_PATTERN.matcher("9").matches());
        assertTrue(JavaUtilDateFormat.TIMESTAMP_PATTERN.matcher("1644894528").matches());
        assertFalse(JavaUtilDateFormat.TIMESTAMP_PATTERN.matcher("01644894528").matches());

        System.out.println("\n------------------------");
        System.out.println(JavaUtilDateFormat.PATTERN_41.parse("2122-01-01 00:00:00", new ParsePosition(0)));
        System.out.println(JavaUtilDateFormat.PATTERN_41.parse("2122-01-01 00:00:00", new ParsePosition(1)));
        System.out.println(JavaUtilDateFormat.DEFAULT.parse("x2122-01-01 00:00:00", new ParsePosition(1)));
        System.out.println(JavaUtilDateFormat.DEFAULT.parse("2122-01-01 00:00:00", new ParsePosition(0)));
        assertEquals("Wed Jan 01 00:00:00 CST 122", JavaUtilDateFormat.DEFAULT.parse("2122-01-01 00:00:00", new ParsePosition(1)).toString());
        assertThrows(ParseException.class, () -> JavaUtilDateFormat.DEFAULT.parse("xxx-01-01 00:00:00"));
    }

    @Test
    public void test3() throws ParseException {
        System.out.println(FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss.SSS", TimeZone.getTimeZone(ZoneOffset.UTC)).format(new Date()));
        System.out.println(FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss.SSS", TimeZone.getTimeZone("GMT+8")).format(new Date()));
        System.out.println("-------");
        System.out.println(FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss.SSS", TimeZone.getTimeZone(ZoneOffset.UTC)).parse("2022-12-15 11:53:12.273"));
        System.out.println(FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss.SSS", TimeZone.getTimeZone("GMT+8")).parse("2022-12-15 11:53:12.273"));
        System.out.println("-------");
        System.out.println(FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSX", TimeZone.getTimeZone(ZoneOffset.UTC)).parse("2022-12-15T11:50:29.855+08"));
        System.out.println(FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSX", TimeZone.getTimeZone("GMT+8")).parse("2022-12-15T11:50:29.855+08"));
    }

}

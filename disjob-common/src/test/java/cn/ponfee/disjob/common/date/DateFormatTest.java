package cn.ponfee.disjob.common.date;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateFormatTest {

    @Test
    public void test1() {
        LocalDate date = LocalDate.of(2014, 3, 18);
        String s1 = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        String s2 = date.format(DateTimeFormatter.ISO_LOCAL_DATE);

        LocalDate date1 = LocalDate.parse("20140318", DateTimeFormatter.BASIC_ISO_DATE);
        LocalDate date2 = LocalDate.parse("2014-03-18", DateTimeFormatter.ISO_LOCAL_DATE);


        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        date1 = LocalDate.of(2014, 3, 18);
        String formattedDate = date1.format(formatter);
        date2 = LocalDate.parse(formattedDate, formatter);

        DateTimeFormatter italianFormatter = new DateTimeFormatterBuilder()
            .appendText(ChronoField.DAY_OF_MONTH)
            .appendLiteral(". ")
            .appendText(ChronoField.MONTH_OF_YEAR)
            .appendLiteral(" ")
            .appendText(ChronoField.YEAR)
            .parseCaseInsensitive()
            .toFormatter(Locale.ITALIAN);

        ZoneId romeZone = ZoneId.of("Europe/Rome");
        date = LocalDate.of(2014, Month.MARCH, 18);
        ZonedDateTime zdt1 = date.atStartOfDay(romeZone);
        LocalDateTime dateTime = LocalDateTime.of(2014, Month.MARCH, 18, 13, 45);
        ZonedDateTime zdt2 = dateTime.atZone(romeZone);
        Instant instant = Instant.now();
        ZonedDateTime zdt3 = instant.atZone(romeZone);
        System.out.println(zdt3);
    }

    @Test
    public void test2() {
        LocalDateTime dateTime = LocalDateTime.of(2014, Month.MARCH, 18, 13, 45);
        Instant instantFromDateTime = dateTime.toInstant(ZoneOffset.UTC);
        System.out.println(instantFromDateTime);
    }

    @Test
    public void test3() {
        System.out.println(Runtime.getRuntime().availableProcessors());
        System.out.println(Lists.newArrayList(1,2,3).stream().reduce(10, Integer::sum));
        System.out.println(Lists.newArrayList(1,2,3).stream().reduce(Integer::sum));
    }

    @Test
    public void test4() throws ParseException {
        Date zero = JavaUtilDateFormat.PATTERN_41.parse(Dates.ZERO_DATE_TIME);
        assertEquals(-62170185600000L, zero.getTime());
        assertEquals(zero, JavaUtilDateFormat.DEFAULT.parse(Dates.ZERO_DATE_TIME));
        assertEquals(zero, DateUtils.parseDate(Dates.ZERO_DATE_TIME, Dates.DATETIME_PATTERN));

    }
}

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

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Convert to {@code java.time.LocalDateTime}, none zone offset.
 * <p>unix timestamp只支持对10位(秒)和13位(毫秒)做解析
 * <p>时区：LocalDateTime[无]、Date[0时区]、Instant[0时区]、ZonedDateTime[自带]
 * <p>线程安全
 *
 * @author Ponfee
 */
public class LocalDateTimeFormat {

    /**
     * Datetime formatter for datetime compact pattern
     */
    public static final DateTimeFormatter DATETIME_COMPACT_FORMATTER = DateTimeFormatter.ofPattern(Dates.DATETIME_COMPACT_PATTERN);

    /**
     * Datetime formatter for datetime milli compact pattern
     */
    public static final DateTimeFormatter DATETIME_MILLI_COMPACT_FORMATTER = DateTimeFormatter.ofPattern(Dates.DATETIME_MILLI_COMPACT_PATTERN);

    /**
     * Timestamp regex pattern
     */
    public static final Pattern TIMESTAMP_PATTERN = Pattern.compile("^(0|[1-9]\\d*)$");

    /**
     * The default date format with yyyy-MM-dd HH:mm:ss
     */
    public static final LocalDateTimeFormat DEFAULT = new LocalDateTimeFormat(Dates.DATETIME_PATTERN);

    /**
     * For {@link Date#toString()} "EEE MMM dd HH:mm:ss zzz yyyy" format
     */
    private static final Pattern CST_PATTERN = Pattern.compile("^(Sun|Mon|Tue|Wed|Thu|Fri|Sat) [A-Z][a-z]{2} \\d{2} \\d{2}:\\d{2}:\\d{2} CST \\d{4}$");

    /**
     * For {@code java.util.Date#toString}
     */
    private static final DateTimeFormatter CST_FORMATTER = DateTimeFormatter.ofPattern(Dates.DATE_TO_STRING_PATTERN, Locale.ROOT);

    /**
     * 兜底解析器
     */
    private final DateTimeFormatter backstopFormat;

    public LocalDateTimeFormat(String pattern) {
        this(DateTimeFormatter.ofPattern(pattern));
    }

    public LocalDateTimeFormat(DateTimeFormatter dateTimeFormatter) {
        this.backstopFormat = dateTimeFormatter;
    }

    // --------------------------------------------------------------------------public methods

    public LocalDateTime parse(String source) {
        int length;
        if (source == null || (length = source.length()) == 0) {
            return null;
        }

        switch (length) {
            case 8:
                // yyyyMMdd
                return LocalDate.parse(source, DateTimeFormatter.BASIC_ISO_DATE).atStartOfDay();
            case 10:
                char literal = source.charAt(4);
                if (literal == '-' || literal == '/') {
                    // yyyy-MM-dd, yyyy/MM/dd
                    return LocalDate.parse(standardizeIsoDate(source)).atStartOfDay();
                }
                if (isTimestamp(source)) {
                    // 10位数字的unix时间戳，如：1640966400
                    return Instant.ofEpochSecond(Long.parseLong(source)).atZone(ZoneId.systemDefault()).toLocalDateTime();
                }
                break;
            case 13:
                if (isTimestamp(source)) {
                    // 13位数字的毫秒时间戳，如：1640966400000
                    return Instant.ofEpochMilli(Long.parseLong(source)).atZone(ZoneId.systemDefault()).toLocalDateTime();
                }
                break;
            case 14:
                // yyyyMMddHHmmss
                return LocalDateTime.parse(source, DATETIME_COMPACT_FORMATTER);
            case 17:
                // yyyyMMddHHmmssSSS
                return LocalDateTime.parse(source, DATETIME_MILLI_COMPACT_FORMATTER);
            default:
                break;
        }

        if (isCst(source)) {
            // Thu Jan 01 00:00:00 CST 1970
            return LocalDateTime.parse(source, CST_FORMATTER);
        }

        if (length >= 19) {
            if (source.endsWith("Z")) {
                // yyyy-MM-dd'T'HH:mm:ss.SSS'Z' -> 2000-01-01T00:00:00.000Z
                return parseToInstant(source).atZone(ZoneId.systemDefault()).toLocalDateTime();
            } else if (source.contains("+")) {
                // yyyy-MM-dd'T'HH:mm:ss.SSSXXX -> 2000-01-01T00:00:00.000+08:00
                return parseToOffsetDateTime(source).toLocalDateTime();
            } else {
                // yyyy-MM-dd'T'HH:mm:ss.SSS    -> 2000-01-01T00:00:00.000
                return parseToLocalDateTime(source);
            }
        }

        return LocalDateTime.parse(source, backstopFormat);
    }

    public String format(LocalDateTime dateTime) {
        return dateTime == null ? null : backstopFormat.format(dateTime);
    }

    // ----------------------------------------------------------------------static methods

    static Instant parseToInstant(String str) {
        return Instant.parse(standardizeIsoDateTime(str));
    }

    static OffsetDateTime parseToOffsetDateTime(String str) {
        return OffsetDateTime.parse(standardizeIsoDateTime(str));
    }

    static LocalDateTime parseToLocalDateTime(String str) {
        return LocalDateTime.parse(standardizeIsoDateTime(str));
    }

    static String standardizeIsoDateTime(String str) {
        if (str.charAt(4) == '-' && str.charAt(10) == 'T') {
            return str;
        }
        // yyyy/MM/dd HH:mm:ss  ->  yyyy-MM-ddTHH:mm:ss
        char[] chars = str.toCharArray();
        chars[4] = '-';
        chars[7] = '-';
        chars[10] = 'T';
        return new String(chars);
    }

    static String standardizeIsoDate(String str) {
        if (str.charAt(4) == '-') {
            return str;
        }
        // yyyy/MM/dd  ->  yyyy-MM-dd
        char[] chars = str.toCharArray();
        chars[4] = '-';
        chars[7] = '-';
        return new String(chars);
    }

    static boolean isTimestamp(String str) {
        return TIMESTAMP_PATTERN.matcher(str).matches();
    }

    static boolean isCst(String str) {
        return str.length() == 28 && CST_PATTERN.matcher(str).matches();
    }

    static LocalDateTime cstToLocalDateTime(String str) {
        return LocalDateTime.parse(str, CST_FORMATTER);
    }

}

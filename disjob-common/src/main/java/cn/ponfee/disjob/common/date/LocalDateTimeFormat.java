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

import cn.ponfee.disjob.common.base.Symbol.Char;

import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Convert to {@code java.time.LocalDateTime}, none zone offset.
 * <p>unix timestamp只支持对10位(秒)和13位(毫秒)做解析
 * <p>时区：LocalDateTime[无]、Date[0时区]、Instant[0时区]、ZonedDateTime[自带]
 * <p>线程安全
 *
 * @author Ponfee
 * @see JavaUtilDateFormat#parseToLocalDateTime(String)
 */
@ThreadSafe
public class LocalDateTimeFormat {

    static final DateTimeFormatter PATTERN_01 = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    static final DateTimeFormatter PATTERN_11 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    static final DateTimeFormatter PATTERN_12 = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    static final DateTimeFormatter PATTERN_13 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    static final DateTimeFormatter PATTERN_14 = DateTimeFormatter.ofPattern("yyyy/MM/dd'T'HH:mm:ss");

    static final DateTimeFormatter PATTERN_21 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    static final DateTimeFormatter PATTERN_22 = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS");
    static final DateTimeFormatter PATTERN_23 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    static final DateTimeFormatter PATTERN_24 = DateTimeFormatter.ofPattern("yyyy/MM/dd'T'HH:mm:ss.SSS");

    static final DateTimeFormatter PATTERN_31 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS'Z'");
    static final DateTimeFormatter PATTERN_32 = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS'Z'");
    static final DateTimeFormatter PATTERN_33 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    static final DateTimeFormatter PATTERN_34 = DateTimeFormatter.ofPattern("yyyy/MM/dd'T'HH:mm:ss.SSS'Z'");

    /**
     * The default date format with yyyy-MM-dd HH:mm:ss
     */
    public static final LocalDateTimeFormat DEFAULT = new LocalDateTimeFormat("yyyy-MM-dd HH:mm:ss");

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

        if (length >= 20 && source.endsWith("Z")) {
            // example:
            //   2022-07-18T15:11:11Z, 2022-07-18T15:11:11.Z, 2022-07-18T15:11:11.1Z, 2022-07-18T15:11:11.13Z, 2022-07-18T15:11:11.133Z
            //   2022/07/18T15:11:11Z, 2022/07/18T15:11:11.Z, 2022/07/18T15:11:11.1Z, 2022/07/18T15:11:11.13Z, 2022/07/18T15:11:11.133Z
            if (length < 24) {
                source = JavaUtilDateFormat.complete(source);
            }
            if (JavaUtilDateFormat.isTSeparator(source)) {
                return LocalDateTime.parse(source, JavaUtilDateFormat.isCrossbar(source) ? PATTERN_33 : PATTERN_34);
            } else {
                return LocalDateTime.parse(source, JavaUtilDateFormat.isCrossbar(source) ? PATTERN_31 : PATTERN_32);
            }
        }

        switch (length) {
            case 8:
                // yyyyMMdd
                return LocalDateTime.parse(source + "000000", PATTERN_01);
            case 10:
                char c = source.charAt(4);
                if (c == Char.HYPHEN) {
                    // yyyy-MM-dd
                    return LocalDateTime.parse(source + " 00:00:00", PATTERN_11);
                } else if (c == Char.SLASH) {
                    // yyyy/MM/dd
                    return LocalDateTime.parse(source + " 00:00:00", PATTERN_12);
                } else if (JavaUtilDateFormat.TIMESTAMP_PATTERN.matcher(source).matches()) {
                    // long string(length 10) of second unix timestamp(e.g. 1640966400)
                    return Dates.toLocalDateTime(new Date(Long.parseLong(source) * 1000));
                }
                break;
            case 13:
                if (JavaUtilDateFormat.TIMESTAMP_PATTERN.matcher(source).matches()) {
                    // long string(length 13) of millisecond unix timestamp(e.g. 1640966400000)
                    return Dates.toLocalDateTime(new Date(Long.parseLong(source)));
                }
                break;
            case 14:
                return LocalDateTime.parse(source, PATTERN_01);
            case 19:
                if (JavaUtilDateFormat.isTSeparator(source)) {
                    return LocalDateTime.parse(source, JavaUtilDateFormat.isCrossbar(source) ? PATTERN_13 : PATTERN_14);
                } else {
                    return LocalDateTime.parse(source, JavaUtilDateFormat.isCrossbar(source) ? PATTERN_11 : PATTERN_12);
                }
            case 23:
                if (JavaUtilDateFormat.isTSeparator(source)) {
                    return LocalDateTime.parse(source, JavaUtilDateFormat.isCrossbar(source) ? PATTERN_23 : PATTERN_24);
                } else {
                    return LocalDateTime.parse(source, JavaUtilDateFormat.isCrossbar(source) ? PATTERN_21 : PATTERN_22);
                }
            default:
                break;
        }

        return LocalDateTime.parse(source, backstopFormat);
    }

    public String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return backstopFormat.format(dateTime);
    }

}

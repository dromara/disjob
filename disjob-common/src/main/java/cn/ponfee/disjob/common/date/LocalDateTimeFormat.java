/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.date;

import cn.ponfee.disjob.common.base.Symbol.Char;

import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static cn.ponfee.disjob.common.date.JavaUtilDateFormat.*;

/**
 * Convert to {@code java.time.LocalDateTime}, none zone offset.
 * <p>unix timestamp只支持对10位(秒)和13位(毫秒)做解析
 * <p>时区：LocalDateTime[无]、Date[0时区]、Instant[0时区]、ZonedDateTime[自带]
 *
 * @author Ponfee
 * @ThreadSafe
 * @see JavaUtilDateFormat#parseToLocalDateTime(String)
 */
@ThreadSafe
public class LocalDateTimeFormat {

    static final DateTimeFormatter PATTERN_01 = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static final DateTimeFormatter PATTERN_11 = DateTimeFormatter.ofPattern(Dates.DATETIME_PATTERN);
    static final DateTimeFormatter PATTERN_12 = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    static final DateTimeFormatter PATTERN_13 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    static final DateTimeFormatter PATTERN_14 = DateTimeFormatter.ofPattern("yyyy/MM/dd'T'HH:mm:ss");

    static final DateTimeFormatter PATTERN_21 = DateTimeFormatter.ofPattern(Dates.DATEFULL_PATTERN);
    static final DateTimeFormatter PATTERN_22 = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS");
    static final DateTimeFormatter PATTERN_23 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    static final DateTimeFormatter PATTERN_24 = DateTimeFormatter.ofPattern("yyyy/MM/dd'T'HH:mm:ss.SSS");

    /**
     * The default date format with yyyy-MM-dd HH:mm:ss
     */
    public static final LocalDateTimeFormat DEFAULT = new LocalDateTimeFormat(Dates.DATETIME_PATTERN);

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
        if (source == null || source.length() == 0) {
            return null;
        }

        int length = source.length();
        if (length >= 20 && hasTSeparator(source) && source.endsWith("Z")) {
            if (isCrossbar(source)) {
                // example: 2022-07-18T15:11:11Z, 2022-07-18T15:11:11.Z, 2022-07-18T15:11:11.1Z, 2022-07-18T15:11:11.13Z, 2022-07-18T15:11:11.133Z
                // 解析会报错：DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                return LocalDateTime.ofInstant(Instant.parse(source), ZoneOffset.UTC);
            } else {
                // example: 2022/07/18T15:11:11Z, 2022/07/18T15:11:11.Z, 2022/07/18T15:11:11.1Z, 2022/07/18T15:11:11.13Z, 2022/07/18T15:11:11.133Z
                source = length < 24 ? padding(source) : source.substring(0, source.length() - 1);
                return LocalDateTime.parse(source, PATTERN_24);
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
                } else if (JavaUtilDateFormat.DATE_TIMESTAMP_PATTERN.matcher(source).matches()) {
                    // long string(length 10) of second unix timestamp(e.g. 1640966400)
                    return Dates.toLocalDateTime(new Date(Long.parseLong(source) * 1000));
                }
                break;
            case 13:
                if (JavaUtilDateFormat.DATE_TIMESTAMP_PATTERN.matcher(source).matches()) {
                    // long string(length 13) of millisecond unix timestamp(e.g. 1640966400000)
                    return Dates.toLocalDateTime(new Date(Long.parseLong(source)));
                }
                break;
            case 14:
                return LocalDateTime.parse(source, PATTERN_01);
            case 19:
                if (hasTSeparator(source)) {
                    return LocalDateTime.parse(source, isCrossbar(source) ? PATTERN_13 : PATTERN_14);
                } else {
                    return LocalDateTime.parse(source, isCrossbar(source) ? PATTERN_11 : PATTERN_12);
                }
            case 23:
                if (hasTSeparator(source)) {
                    return LocalDateTime.parse(source, isCrossbar(source) ? PATTERN_23 : PATTERN_24);
                } else {
                    return LocalDateTime.parse(source, isCrossbar(source) ? PATTERN_21 : PATTERN_22);
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

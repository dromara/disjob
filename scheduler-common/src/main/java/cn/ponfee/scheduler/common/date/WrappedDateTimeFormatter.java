package cn.ponfee.scheduler.common.date;

import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static cn.ponfee.scheduler.common.date.WrappedFastDateFormat.*;

/**
 * Convert to {@code java.time.LocalDateTime}
 *
 * <p>unix timestamp只支持对10位(秒)和13位(毫秒)做解析
 *
 * @author Ponfee
 * @ThreadSafe
 */
@ThreadSafe
public class WrappedDateTimeFormatter {

    public static final DateTimeFormatter PATTERN_01 = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static final DateTimeFormatter PATTERN_11 = DateTimeFormatter.ofPattern(Dates.DEFAULT_DATE_FORMAT);
    public static final DateTimeFormatter PATTERN_12 = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    public static final DateTimeFormatter PATTERN_13 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    public static final DateTimeFormatter PATTERN_14 = DateTimeFormatter.ofPattern("yyyy/MM/dd'T'HH:mm:ss");

    public static final DateTimeFormatter PATTERN_21 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    public static final DateTimeFormatter PATTERN_22 = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS");
    public static final DateTimeFormatter PATTERN_23 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    public static final DateTimeFormatter PATTERN_24 = DateTimeFormatter.ofPattern("yyyy/MM/dd'T'HH:mm:ss.SSS");


    /**
     * The default date format with yyyy-MM-dd HH:mm:ss
     */
    public static final WrappedDateTimeFormatter DEFAULT = new WrappedDateTimeFormatter(Dates.DEFAULT_DATE_FORMAT);

    /**
     * 兜底解析器
     */
    private final DateTimeFormatter backstopFormatter;

    /**
     * 时区偏移量：UTC为0
     *
     * @see ZoneOffset#ofHours(int)
     */
    private final ZoneOffset zoneOffset;

    public WrappedDateTimeFormatter() {
        this(Dates.DEFAULT_DATE_FORMAT);
    }

    public WrappedDateTimeFormatter(String pattern) {
        this(DateTimeFormatter.ofPattern(pattern));
    }

    public WrappedDateTimeFormatter(DateTimeFormatter dateTimeFormatter) {
        // ZoneOffset.ofHours(8)
        this(dateTimeFormatter, ZoneOffset.UTC);
    }

    public WrappedDateTimeFormatter(String pattern, ZoneOffset zoneOffset) {
        this(DateTimeFormatter.ofPattern(pattern), zoneOffset);
    }

    public WrappedDateTimeFormatter(DateTimeFormatter dateTimeFormatter, ZoneOffset zoneOffset) {
        this.backstopFormatter = dateTimeFormatter;
        this.zoneOffset = zoneOffset;
    }

    public DateTimeFormatter getBackstopFormatter() {
        return backstopFormatter;
    }

    public ZoneOffset getZoneOffset() {
        return zoneOffset;
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
                return LocalDateTime.ofInstant(Instant.parse(source), zoneOffset);
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
                if (c == '-') {
                    // yyyy-MM-dd
                    return LocalDateTime.parse(source + " 00:00:00", PATTERN_11);
                } else if (c == '/') {
                    // yyyy/MM/dd
                    return LocalDateTime.parse(source + " 00:00:00", PATTERN_12);
                } else if (WrappedFastDateFormat.DATE_TIMESTAMP_PATTERN.matcher(source).matches()) {
                    // long string(length 10) of second unix timestamp(e.g. 1640966400)
                    return new Date(Long.parseLong(source) * 1000).toInstant().atZone(zoneOffset).toLocalDateTime();
                }
                break;
            case 13:
                if (WrappedFastDateFormat.DATE_TIMESTAMP_PATTERN.matcher(source).matches()) {
                    // long string(length 13) of millisecond unix timestamp(e.g. 1640966400000)
                    return new Date(Long.parseLong(source)).toInstant().atZone(zoneOffset).toLocalDateTime();
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

        return LocalDateTime.parse(source, backstopFormatter);
    }

    public String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return backstopFormatter.format(dateTime);
    }

}

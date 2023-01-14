/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.date;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import javax.annotation.concurrent.ThreadSafe;
import java.text.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Convert to {@code java.util.Date}, none zone offset.
 * <p>unix timestamp只支持对10位(秒)和13位(毫秒)做解析
 *
 * @author Ponfee
 * @ThreadSafe
 */
@ThreadSafe
public class JavaUtilDateFormat extends DateFormat {

    private static final long serialVersionUID = 6837172676882367405L;

    /**
     * For {@code java.util.Date#toString}
     */
    private static final DateTimeFormatter DATE_TO_STRING_FORMAT = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ROOT);

    /**
     * For {@link Date#toString()} "EEE MMM dd HH:mm:ss zzz yyyy" format
     */
    static final Pattern DATE_TO_STRING_PATTERN = Pattern.compile("^(Sun|Mon|Tue|Wed|Thu|Fri|Sat) [A-Z][a-z]{2} \\d{2} \\d{2}:\\d{2}:\\d{2} CST \\d{4}$");

    /**
     * 日期时间戳：秒/毫秒
     */
    static final Pattern DATE_TIMESTAMP_PATTERN = Pattern.compile("^0|[1-9]\\d*$");

    static final FastDateFormat PATTERN_11 = FastDateFormat.getInstance("yyyyMM");
    static final FastDateFormat PATTERN_12 = FastDateFormat.getInstance("yyyy-MM");
    static final FastDateFormat PATTERN_13 = FastDateFormat.getInstance("yyyy/MM");

    static final FastDateFormat PATTERN_21 = FastDateFormat.getInstance("yyyyMMdd");
    static final FastDateFormat PATTERN_22 = FastDateFormat.getInstance("yyyy-MM-dd");
    static final FastDateFormat PATTERN_23 = FastDateFormat.getInstance("yyyy/MM/dd");

    static final FastDateFormat PATTERN_31 = FastDateFormat.getInstance("yyyyMMddHHmmss");
    static final FastDateFormat PATTERN_32 = FastDateFormat.getInstance("yyyyMMddHHmmssSSS");

    static final FastDateFormat PATTERN_41 = FastDateFormat.getInstance(Dates.DEFAULT_DATE_FORMAT);
    static final FastDateFormat PATTERN_42 = FastDateFormat.getInstance("yyyy/MM/dd HH:mm:ss");
    static final FastDateFormat PATTERN_43 = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss");
    static final FastDateFormat PATTERN_44 = FastDateFormat.getInstance("yyyy/MM/dd'T'HH:mm:ss");

    static final FastDateFormat PATTERN_51 = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss.SSS");
    static final FastDateFormat PATTERN_52 = FastDateFormat.getInstance("yyyy/MM/dd HH:mm:ss.SSS");
    static final FastDateFormat PATTERN_53 = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSS");
    static final FastDateFormat PATTERN_54 = FastDateFormat.getInstance("yyyy/MM/dd'T'HH:mm:ss.SSS");

    static final FastDateFormat PATTERN_61 = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss.SSS'Z'");
    static final FastDateFormat PATTERN_62 = FastDateFormat.getInstance("yyyy/MM/dd HH:mm:ss.SSS'Z'");
    static final FastDateFormat PATTERN_63 = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    static final FastDateFormat PATTERN_64 = FastDateFormat.getInstance("yyyy/MM/dd'T'HH:mm:ss.SSS'Z'");

    static final FastDateFormat PATTERN_71 = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss.SSSX");
    static final FastDateFormat PATTERN_72 = FastDateFormat.getInstance("yyyy/MM/dd HH:mm:ss.SSSX");
    static final FastDateFormat PATTERN_73 = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSX");
    static final FastDateFormat PATTERN_74 = FastDateFormat.getInstance("yyyy/MM/dd'T'HH:mm:ss.SSSX");

    /**
     * The default date format with yyyy-MM-dd HH:mm:ss
     */
    public static final JavaUtilDateFormat DEFAULT = new JavaUtilDateFormat(Dates.DEFAULT_DATE_FORMAT);

    /**
     * 兜底解析器
     */
    private final FastDateFormat backstopFormat;

    public JavaUtilDateFormat(String pattern) {
        this(pattern, Locale.getDefault());
    }

    public JavaUtilDateFormat(String pattern, Locale locale) {
        this(FastDateFormat.getInstance(pattern, locale));
    }

    public JavaUtilDateFormat(FastDateFormat format) {
        this.backstopFormat = format;

        super.setCalendar(Calendar.getInstance(format.getTimeZone(), format.getLocale()));

        NumberFormat numberFormat = NumberFormat.getIntegerInstance(format.getLocale());
        numberFormat.setGroupingUsed(false);
        super.setNumberFormat(numberFormat);
    }

    @Override
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
        return backstopFormat.format(date, toAppendTo, fieldPosition);
    }

    @Override
    public Date parse(String source, ParsePosition pos) {
        Objects.requireNonNull(pos);
        if (pos.getIndex() < 0) {
            throw new IllegalArgumentException("Invalid parse position: " + pos.getIndex());
        }
        if (StringUtils.isEmpty(source) || source.length() <= pos.getIndex()) {
            return null;
        }

        String date = source.substring(pos.getIndex());
        try {
            return parse(date);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid date format: " + source + ", " + pos.getIndex() + ", " + date);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format: " + source + ", " + pos.getIndex() + ", " + date, e);
        }
    }

    public LocalDateTime parseToLocalDateTime(String source, ParsePosition pos) {
        Date date = parse(source, pos);
        return date == null ? null : Dates.toLocalDateTime(date);
    }

    @Override
    public Date parse(String source) throws ParseException {
        if (StringUtils.isEmpty(source)) {
            return null;
        }

        int length = source.length();
        if (length >= 20 && source.endsWith("Z")) {
            if (length < 24) {
                source = padding(source) + "Z";
            }
            if (hasTSeparator(source)) {
                return (isCrossbar(source) ? PATTERN_63 : PATTERN_64).parse(source);
            } else {
                return (isCrossbar(source) ? PATTERN_61 : PATTERN_62).parse(source);
            }
        }

        switch (length) {
            case  6: return PATTERN_11.parse(source);
            case  7: return (isCrossbar(source) ? PATTERN_12 : PATTERN_13).parse(source);
            case  8: return PATTERN_21.parse(source);
            case 10:
                char separator = source.charAt(4);
                if (separator == '-') {
                    return PATTERN_22.parse(source);
                } else if (separator == '/') {
                    return PATTERN_23.parse(source);
                } else if (DATE_TIMESTAMP_PATTERN.matcher(source).matches()) {
                    // long string(length 10) of unix timestamp(e.g. 1640966400)
                    return new Date(Long.parseLong(source) * 1000);
                }
                break;
            case 13:
                // long string(length 13) of mills unix timestamp(e.g. 1640966400000)
                if (DATE_TIMESTAMP_PATTERN.matcher(source).matches()) {
                    return new Date(Long.parseLong(source));
                }
                break;
            case 14: return PATTERN_31.parse(source);
            case 19:
                if (hasTSeparator(source)) {
                    return (isCrossbar(source) ? PATTERN_43 : PATTERN_44).parse(source);
                } else {
                    return (isCrossbar(source) ? PATTERN_41 : PATTERN_42).parse(source);
                }
            case 17: return PATTERN_32.parse(source);
            case 23:
                if (hasTSeparator(source)) {
                    return (isCrossbar(source) ? PATTERN_53 : PATTERN_54).parse(source);
                } else {
                    return (isCrossbar(source) ? PATTERN_51 : PATTERN_52).parse(source);
                }
            case 26: 
            case 29:
                if (hasTSeparator(source)) {
                    // 2021-12-31T17:01:01.000+08、2021-12-31T17:01:01.000+08:00
                    return (isCrossbar(source) ? PATTERN_73 : PATTERN_74).parse(source);
                } else {
                    // 2021-12-31 17:01:01.000+08、2021-12-31 17:01:01.000+08:00
                    return (isCrossbar(source) ? PATTERN_71 : PATTERN_72).parse(source);
                }
            case 28:
                if (isCST(source)) {
                    // 以下使用方式会相差14小时：
                    //   1）FastDateFormat.getInstance("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH).parse(source);
                    //   2）new Date(source);
                    //   3）Date.from(ZonedDateTime.parse(source, DATE_TO_STRING_FORMAT).toInstant());
                    return Dates.toDate(LocalDateTime.parse(source, DATE_TO_STRING_FORMAT));
                }
                break;
            default: break;
        }

        return backstopFormat.parse(source);
    }

    public LocalDateTime parseToLocalDateTime(String source) throws ParseException {
        Date date = parse(source);
        return date == null ? null : Dates.toLocalDateTime(date);
    }

    @Override
    public Object parseObject(String source, ParsePosition pos) {
        return parse(source, pos);
    }

    @Override
    public Object parseObject(String source) throws ParseException {
        return parse(source);
    }

    @Override
    public int hashCode() {
        return backstopFormat.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof JavaUtilDateFormat)) {
            return false;
        }

        JavaUtilDateFormat other = (JavaUtilDateFormat) obj;
        return this.backstopFormat.equals(other.backstopFormat);
    }

    @Override
    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        return backstopFormat.formatToCharacterIterator(obj);
    }

    @Override
    public Object clone() {
        return this;
    }

    // ------------------------------------------------------------------------deprecated methods

    @Override @Deprecated
    public void setCalendar(Calendar newCalendar) {
        if (!Objects.equals(newCalendar, super.getCalendar())) {
            throw new UnsupportedOperationException();
        }
    }

    @Override @Deprecated
    public void setNumberFormat(NumberFormat newNumberFormat) {
        if (!Objects.equals(newNumberFormat, super.getNumberFormat())) {
            throw new UnsupportedOperationException();
        }
    }

    @Override @Deprecated
    public void setTimeZone(TimeZone zone) {
        if (!Objects.equals(zone, super.getTimeZone())) {
            throw new UnsupportedOperationException();
        }
    }

    @Override @Deprecated
    public void setLenient(boolean lenient) {
        if (lenient != super.isLenient()) {
            throw new UnsupportedOperationException();
        }
    }

    // ------------------------------------------------------------------------package methods
    static boolean isCrossbar(String str) {
        return str.charAt(4) == '-';
    }

    // 'T' literal is the date and time separator
    static boolean hasTSeparator(String str) {
        return str.charAt(10) == 'T';
    }

    static boolean isCST(String str) {
        return DATE_TO_STRING_PATTERN.matcher(str).matches();
    }

    static String padding(String source) {
        // example: 2022/07/18T15:11:11Z, 2022/07/18T15:11:11.Z, 2022/07/18T15:11:11.1Z, 2022/07/18T15:11:11.13Z
        String[] array = source.split("[\\.Z]");
        return array[0] + "." + (array.length == 1 ? "000" : Strings.padEnd(array[1], 3, '0'));
    }

}

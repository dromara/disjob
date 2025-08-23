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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.text.*;
import java.util.*;

/**
 * <pre>
 * Convert to {@code java.util.Date}, none zone offset, Thread safe.
 * unix timestamp只支持对10位(秒)和13位(毫秒)做解析
 * yyyy-MM-dd'T'HH:mm:ss.SSS'Z'：ISO 8601标准中的一种日期格式(UTC)，T表示日期和时间的分隔符，Z表示零时区(即UTC+0)
 * </pre>
 *
 * @author Ponfee
 */
public class JavaUtilDateFormat extends DateFormat {

    private static final long serialVersionUID = 6837172676882367405L;

    /**
     * The default date format with yyyy-MM-dd HH:mm:ss
     */
    public static final JavaUtilDateFormat DEFAULT = new JavaUtilDateFormat(Dates.DATETIME_PATTERN);

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

    @Override
    public Date parse(String source) throws ParseException {
        int length;
        if (source == null || (length = source.length()) == 0) {
            return null;
        }

        switch (length) {
            case 8:
                // yyyyMMdd
                return Dates.DATE_COMPACT_FORMAT.parse(source);
            case 10:
                char literal = source.charAt(4);
                if (literal == '-' || literal == '/') {
                    // yyyy-MM-dd, yyyy/MM/dd
                    return Dates.DATE_FORMAT.parse(LocalDateTimeFormat.standardizeIsoDate(source));
                }
                if (LocalDateTimeFormat.isTimestamp(source)) {
                    // 10位数字的unix时间戳，如：1640966400
                    return new Date(Long.parseLong(source) * 1000);
                }
                break;
            case 13:
                if (LocalDateTimeFormat.isTimestamp(source)) {
                    // 13位数字的毫秒时间戳，如：1640966400000
                    return new Date(Long.parseLong(source));
                }
                break;
            case 14:
                // yyyyMMddHHmmss
                return Dates.DATETIME_COMPACT_FORMAT.parse(source);
            case 17:
                // yyyyMMddHHmmssSSS
                return Dates.DATETIME_MILLI_COMPACT_FORMAT.parse(source);
            default:
                break;
        }

        if (LocalDateTimeFormat.isCst(source)) {
            // Thu Jan 01 00:00:00 CST 1970，使用`new Date(source)`方式会晚14小时(Thu Jan 01 14:00:00 CST 1970)
            return Dates.toDate(LocalDateTimeFormat.cstToLocalDateTime(source));
        }

        if (length >= 19) {
            if (source.endsWith("Z")) {
                // yyyy-MM-dd'T'HH:mm:ss.SSS'Z' -> 2000-01-01T00:00:00.000Z
                return Date.from(LocalDateTimeFormat.parseToInstant(source));
            } else if (source.contains("+")) {
                // yyyy-MM-dd'T'HH:mm:ss.SSSXXX -> 2000-01-01T00:00:00.000+08:00
                return Date.from(LocalDateTimeFormat.parseToOffsetDateTime(source).toInstant());
            } else {
                // yyyy-MM-dd'T'HH:mm:ss.SSS    -> 2000-01-01T00:00:00.000
                return Dates.toDate(LocalDateTimeFormat.parseToLocalDateTime(source));
            }
        }

        return backstopFormat.parse(source);
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
        return this.backstopFormat.equals(((JavaUtilDateFormat) obj).backstopFormat);
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

    @Deprecated
    @Override
    public void setCalendar(Calendar newCalendar) {
        if (!Objects.equals(newCalendar, super.getCalendar())) {
            throw new UnsupportedOperationException();
        }
    }

    @Deprecated
    @Override
    public void setNumberFormat(NumberFormat newNumberFormat) {
        if (!Objects.equals(newNumberFormat, super.getNumberFormat())) {
            throw new UnsupportedOperationException();
        }
    }

    @Deprecated
    @Override
    public void setTimeZone(TimeZone zone) {
        if (zone == null && super.getTimeZone() == null) {
            return;
        }
        if (zone == null || super.getTimeZone() == null) {
            throw new UnsupportedOperationException("JavaUtilDateFormat: invalid null time zone.");
        }
        if (zone.getRawOffset() != super.getTimeZone().getRawOffset()) {
            throw new UnsupportedOperationException("JavaUtilDateFormat: Not a same time zone.");
        }
    }

    @Deprecated
    @Override
    public void setLenient(boolean lenient) {
        if (lenient != super.isLenient()) {
            throw new UnsupportedOperationException();
        }
    }

}

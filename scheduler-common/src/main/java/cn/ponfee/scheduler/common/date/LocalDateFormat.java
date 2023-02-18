/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.common.date;

import cn.ponfee.scheduler.common.base.Constants;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Convert to {@code java.time.LocalDate}, none zone offset.
 *
 * @author Ponfee
 * @ThreadSafe
 */
@ThreadSafe
public class LocalDateFormat {

    private static final DateTimeFormatter PATTERN_01 = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter PATTERN_02 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter PATTERN_03 = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * The default date format with yyyy-MM-dd
     */
    public static final LocalDateFormat DEFAULT = new LocalDateFormat("yyyy-MM-dd");

    /**
     * 兜底解析器
     */
    private final DateTimeFormatter backstopFormat;

    public LocalDateFormat(String pattern) {
        this(DateTimeFormatter.ofPattern(pattern));
    }

    public LocalDateFormat(DateTimeFormatter dateTimeFormatter) {
        this.backstopFormat = dateTimeFormatter;
    }

    // --------------------------------------------------------------------------public methods

    public LocalDate parse(String source) {
        if (StringUtils.isBlank(source)) {
            return null;
        }

        int length = source.length();
        switch (length) {
            case 8:
                return LocalDate.parse(source, PATTERN_01);
            case 10:
                char c = source.charAt(4);
                if (c == Constants.HYPHEN) {
                    return LocalDate.parse(source, PATTERN_02);
                } else if (c == Constants.SLASH) {
                    return LocalDate.parse(source, PATTERN_03);
                }
                break;
            default:
                break;
        }
        throw new IllegalArgumentException("Invalid local date format: " + source);
    }

    public String format(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return backstopFormat.format(localDate);
    }

}

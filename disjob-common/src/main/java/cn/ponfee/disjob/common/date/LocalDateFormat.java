/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.date;

import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Convert to {@code java.time.LocalDate}, none zone offset.
 * <p>线程安全
 *
 * @author Ponfee
 */
@ThreadSafe
public class LocalDateFormat {

    /**
     * The default date format with yyyy-MM-dd
     */
    public static final LocalDateFormat DEFAULT = new LocalDateFormat(Dates.DATE_PATTERN);

    private final LocalDateTimeFormat formatter;

    public LocalDateFormat(String pattern) {
        this(DateTimeFormatter.ofPattern(pattern));
    }

    public LocalDateFormat(DateTimeFormatter dateTimeFormatter) {
        this.formatter = new LocalDateTimeFormat(dateTimeFormatter);
    }

    // --------------------------------------------------------------------------public methods

    public LocalDate parse(String source) {
        LocalDateTime localDateTime = formatter.parse(source);
        return localDateTime == null ? null : localDateTime.toLocalDate();
    }

    public String format(LocalDate localDate) {
        return localDate == null ? null : formatter.format(localDate.atStartOfDay());
    }

}

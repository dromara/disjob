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

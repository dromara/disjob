/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application.converter;

import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.date.JavaUtilDateFormat;

import java.text.ParseException;
import java.util.Date;

/**
 * Mapstruct mapper
 *
 * @author Ponfee
 */
public class MapstructMapper {

    public String asString(Date date) {
        return Dates.format(date);
    }

    public Date asDate(String date) {
        try {
            return JavaUtilDateFormat.DEFAULT.parse(date);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid date pattern string: " + date);
        }
    }

}

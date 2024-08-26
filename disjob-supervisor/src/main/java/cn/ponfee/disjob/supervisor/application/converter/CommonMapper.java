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

package cn.ponfee.disjob.supervisor.application.converter;

import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.date.JavaUtilDateFormat;

import java.text.ParseException;
import java.util.Date;

/**
 * Common data type convert mapper
 *
 * @author Ponfee
 */
public class CommonMapper {

    public static String asString(Date date) {
        return Dates.format(date);
    }

    public static Date asDate(String date) {
        try {
            return JavaUtilDateFormat.DEFAULT.parse(date);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid date pattern string: " + date);
        }
    }

    public static Long timeDuration(Date startTime, Date endTime) {
        if (startTime == null || endTime == null) {
            return null;
        }
        return endTime.getTime() - startTime.getTime();
    }

}

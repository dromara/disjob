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

package cn.ponfee.disjob.supervisor.model;

import cn.ponfee.disjob.common.date.DatePeriods;
import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.date.JavaUtilDateFormat;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.model.PeriodTriggerValue;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.supervisor.application.converter.SchedJobConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author Ponfee
 */
public class TriggerValueTest {

    @Test
    public void testValue() throws ParseException {
        Assertions.assertThrows(IllegalArgumentException.class, () -> DatePeriods.valueOf("ABC"));

        String conf = "{\"period\":\"DAILY\", \"start\":\"2000-01-01 00:00:00\", \"step\":2}";
        PeriodTriggerValue triggerValue = Jsons.fromJson(conf, PeriodTriggerValue.class);
        Assertions.assertEquals(triggerValue.getPeriod(), DatePeriods.DAILY);
        Assertions.assertEquals(triggerValue.getStep(), 2);
        Assertions.assertEquals(triggerValue.getStart(), JavaUtilDateFormat.DEFAULT.parse("2000-01-01 00:00:00"));
    }

    @Test
    public void testInt() {
        // 30天的毫秒数转int会溢出，要使用long类型
        int num = 30 * 86400 * 1000;
        Assertions.assertTrue(num < 0);
        Assertions.assertEquals(365, TimeUnit.DAYS.toMillis(365) / (1000 * 86400));
    }

    @Test
    public void testDuration() {
        Date start = Dates.toDate("2000-01-01 01:02:03.456", Dates.DATEFULL_PATTERN);
        Date end = Dates.toDate("2000-01-01 01:02:04.123", Dates.DATEFULL_PATTERN);
        SchedInstance schedInstance = new SchedInstance();
        schedInstance.setRunStartTime(start);
        schedInstance.setRunEndTime(end);
        System.out.println(SchedJobConverter.INSTANCE.convert(schedInstance).getRunDuration());
        Assertions.assertEquals(667, SchedJobConverter.INSTANCE.convert(schedInstance).getRunDuration());
    }

}

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

package cn.ponfee.disjob.supervisor.util;

import cn.ponfee.disjob.common.date.*;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.enums.TriggerType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.Date;

/**
 * CronExpressionTest
 *
 * @author Ponfee
 */
public class CronExpressionTest {

    @Test
    public void testCronExp() {
        Assertions.assertTrue(TriggerType.CRON.validate("0 0 10,14,16 * * ?"));
        Assertions.assertTrue(TriggerType.CRON.validate("0 0 12 ? * WED"));
        Assertions.assertFalse(TriggerType.CRON.validate("0 0 25 ? * WED"));
        Assertions.assertFalse(TriggerType.CRON.validate("0 0 25 ? * WED"));

        Assertions.assertTrue(CronExpression.isValidExpression("43 31 14 31 5 ? 2022"));

        Date date = Dates.random(Dates.ofTimeMillis(0L), Dates.ofTimeMillis(new Date().getTime()));
        Assertions.assertTrue(CronExpression.isValidExpression(Dates.toCronExpression(date)));
        Assertions.assertFalse(org.springframework.scheduling.support.CronExpression.isValidExpression(Dates.toCronExpression(date)));
    }

    @Test
    public void testTrigger() {
        Assertions.assertTrue(TriggerType.CRON.validate("0 0 10,14,16 * * ?"));
        Assertions.assertTrue(TriggerType.ONCE.validate("2022-05-31 14:31:43"));

        Assertions.assertFalse(TriggerType.ONCE.validate("0 0 10,14,16 * * ?"));
        Assertions.assertFalse(TriggerType.CRON.validate("2022-05-31 14:31:43"));

        DatePeriodValue value = new DatePeriodValue();
        value.setPeriod(DatePeriods.DAILY);
        value.setStart(new Date());
        value.setStep(2);
        Assertions.assertTrue(TriggerType.PERIOD.validate(Jsons.toJson(value)));

        Assertions.assertFalse(TriggerType.PERIOD.validate("{\"period\":\"DAILYx\", \"start\":\"2000-01-01 00:00:00\", \"step\":1}"));
        Assertions.assertFalse(TriggerType.PERIOD.validate("{\"period\":\"DAILY\", \"start\":\"2000-x-01 00:00:00\", \"step\":1}"));
        Assertions.assertFalse(TriggerType.PERIOD.validate("{\"period\":\"DAILY\", \"start\":\"2000-5-01 00:00:00\", \"step\":\"x\"}"));
    }

    @Test
    public void testComputeNextTriggerTimeCRON() throws ParseException {
        String triggerValue = "0 0 10,14,16 * * ?";
        Date d1 = TriggerType.CRON.computeNextTriggerTime(triggerValue, JavaUtilDateFormat.DEFAULT.parse("2022-06-01 12:31:34"));
        Assertions.assertEquals("2022-06-01 14:00:00", Dates.format(d1));

        Date d2 = TriggerType.CRON.computeNextTriggerTime(triggerValue, d1);
        Assertions.assertEquals("2022-06-01 16:00:00", Dates.format(d2));
    }

    @Test
    public void testComputeNextTriggerTimeONCE() throws ParseException {
        String triggerValue = "2022-06-01 10:00:00";
        Assertions.assertNull(TriggerType.ONCE.computeNextTriggerTime(triggerValue, JavaUtilDateFormat.DEFAULT.parse("2022-06-01 10:00:01")));

        Date d1 = TriggerType.ONCE.computeNextTriggerTime(triggerValue, JavaUtilDateFormat.DEFAULT.parse("2022-05-31 10:00:01"));
        Assertions.assertEquals("2022-06-01 10:00:00", Dates.format(d1));
        Assertions.assertNull(TriggerType.ONCE.computeNextTriggerTime(triggerValue, d1));
    }

    @Test
    public void testComputeNextTriggerTimePERIOD() throws ParseException {
        String triggerValue = "{\"period\":\"MONTHLY\", \"start\":\"2022-05-01 00:00:00\", \"step\":1}";
        Assertions.assertFalse(TriggerType.PERIOD.computeNextTriggerTime(triggerValue, new Date()).before(new Date()));

        Date startTime = JavaUtilDateFormat.DEFAULT.parse("2022-05-01 00:00:00");
        Assertions.assertEquals("2022-06-01 00:00:00", Dates.format(TriggerType.PERIOD.computeNextTriggerTime(triggerValue, startTime)));

        startTime = JavaUtilDateFormat.DEFAULT.parse("2022-05-31 23:59:58");
        Assertions.assertEquals("2022-06-01 00:00:00", Dates.format(TriggerType.PERIOD.computeNextTriggerTime(triggerValue, startTime)));
    }

}

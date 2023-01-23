/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.test.job.util;

import cn.ponfee.scheduler.common.date.CronExpression;
import cn.ponfee.scheduler.common.date.DatePeriods;
import cn.ponfee.scheduler.common.date.Dates;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.enums.TriggerType;
import cn.ponfee.scheduler.core.model.PeriodTriggerValue;
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
    public void testCronExp() throws ParseException {
        Assertions.assertTrue(TriggerType.CRON.isValid("0 0 10,14,16 * * ?"));
        Assertions.assertTrue(TriggerType.CRON.isValid("0 0 12 ? * WED"));
        Assertions.assertFalse(TriggerType.CRON.isValid("0 0 25 ? * WED"));
        Assertions.assertFalse(TriggerType.CRON.isValid("0 0 25 ? * WED"));

        Assertions.assertTrue(CronExpression.isValidExpression("43 31 14 31 5 ? 2022"));

        Date date = Dates.random(Dates.ofMillis(0L), Dates.ofMillis(new Date().getTime()));
        Assertions.assertTrue(CronExpression.isValidExpression(Dates.toCronExpression(date)));
        Assertions.assertFalse(org.springframework.scheduling.support.CronExpression.isValidExpression(Dates.toCronExpression(date)));
    }

    @Test
    public void testTrigger() {
        Assertions.assertTrue(TriggerType.CRON.isValid("0 0 10,14,16 * * ?"));
        Assertions.assertTrue(TriggerType.ONCE.isValid("2022-05-31 14:31:43"));

        Assertions.assertFalse(TriggerType.ONCE.isValid("0 0 10,14,16 * * ?"));
        Assertions.assertFalse(TriggerType.CRON.isValid("2022-05-31 14:31:43"));

        PeriodTriggerValue value = new PeriodTriggerValue();
        value.setPeriod(DatePeriods.DAILY);
        value.setStart(new Date());
        value.setStep(2);
        Assertions.assertTrue(TriggerType.PERIOD.isValid(Jsons.toJson(value)));

        Assertions.assertFalse(TriggerType.PERIOD.isValid("{\"period\":\"DAILYx\", \"start\":\"2000-01-01 00:00:00\", \"step\":1}"));
        Assertions.assertFalse(TriggerType.PERIOD.isValid("{\"period\":\"DAILY\", \"start\":\"2000-x-01 00:00:00\", \"step\":1}"));
        Assertions.assertFalse(TriggerType.PERIOD.isValid("{\"period\":\"DAILY\", \"start\":\"2000-5-01 00:00:00\", \"step\":\"x\"}"));
    }

    @Test
    public void testComputeNextFireTimeCRON() {
        String triggerValue = "0 0 10,14,16 * * ?";
        Date d1 = TriggerType.CRON.computeNextFireTime(triggerValue, Dates.toDate("2022-06-01 12:31:34"));
        Assertions.assertEquals("2022-06-01 14:00:00", Dates.format(d1));

        Date d2 = TriggerType.CRON.computeNextFireTime(triggerValue, d1);
        Assertions.assertEquals("2022-06-01 16:00:00", Dates.format(d2));
    }

    @Test
    public void testComputeNextFireTimeONCE() {
        String triggerValue = "2022-06-01 10:00:00";
        Assertions.assertNull(TriggerType.ONCE.computeNextFireTime(triggerValue, Dates.toDate("2022-06-01 10:00:01")));

        Date d1 = TriggerType.ONCE.computeNextFireTime(triggerValue, Dates.toDate("2022-05-31 10:00:01"));
        Assertions.assertEquals("2022-06-01 10:00:00", Dates.format(d1));
        Assertions.assertNull(TriggerType.ONCE.computeNextFireTime(triggerValue, d1));
    }

    @Test
    public void testComputeNextFireTimePERIOD() {
        String triggerValue = "{\"period\":\"MONTHLY\", \"start\":\"2022-05-01 00:00:00\", \"step\":1}";
        Assertions.assertFalse(TriggerType.PERIOD.computeNextFireTime(triggerValue, new Date()).before(new Date()));

        Date startTime = Dates.toDate("2022-05-01 00:00:00");
        Assertions.assertEquals("2022-06-01 00:00:00", Dates.format(TriggerType.PERIOD.computeNextFireTime(triggerValue, startTime)));

        startTime = Dates.toDate("2022-05-31 23:59:58");
        Assertions.assertEquals("2022-06-01 00:00:00", Dates.format(TriggerType.PERIOD.computeNextFireTime(triggerValue, startTime)));
    }

}

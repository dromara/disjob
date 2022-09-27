package cn.ponfee.scheduler.supervisor.test.job.util;

import cn.ponfee.scheduler.common.date.CronExpression;
import cn.ponfee.scheduler.common.date.DatePeriods;
import cn.ponfee.scheduler.common.date.Dates;
import cn.ponfee.scheduler.core.enums.TriggerType;
import cn.ponfee.scheduler.core.model.PeriodTriggerConf;
import com.alibaba.fastjson.JSON;
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

        Date date = Dates.random(Dates.ofMillis(0), Dates.ofMillis(new Date().getTime()));
        Assertions.assertTrue(CronExpression.isValidExpression(CronExpression.toCronExpression(date)));
    }

    @Test
    public void testTrigger() {
        Assertions.assertTrue(TriggerType.CRON.isValid("0 0 10,14,16 * * ?"));
        Assertions.assertTrue(TriggerType.ONCE.isValid("2022-05-31 14:31:43"));

        Assertions.assertFalse(TriggerType.ONCE.isValid("0 0 10,14,16 * * ?"));
        Assertions.assertFalse(TriggerType.CRON.isValid("2022-05-31 14:31:43"));

        PeriodTriggerConf conf = new PeriodTriggerConf();
        conf.setPeriod(DatePeriods.DAILY);
        conf.setStart(new Date());
        conf.setStep(2);
        Assertions.assertTrue(TriggerType.PERIOD.isValid(JSON.toJSONString(conf)));

        Assertions.assertFalse(TriggerType.PERIOD.isValid("{\"period\":\"DAILYx\", \"start\":\"2000-01-01 00:00:00\", \"step\":1}"));
        Assertions.assertFalse(TriggerType.PERIOD.isValid("{\"period\":\"DAILY\", \"start\":\"2000-x-01 00:00:00\", \"step\":1}"));
        Assertions.assertFalse(TriggerType.PERIOD.isValid("{\"period\":\"DAILY\", \"start\":\"2000-5-01 00:00:00\", \"step\":\"x\"}"));
    }

    @Test
    public void testComputeNextFireTimeCRON() {
        String triggerConf = "0 0 10,14,16 * * ?";
        Date d1 = TriggerType.CRON.computeNextFireTime(triggerConf, Dates.toDate("2022-06-01 12:31:34"));
        Assertions.assertEquals("2022-06-01 14:00:00", Dates.format(d1));

        Date d2 = TriggerType.CRON.computeNextFireTime(triggerConf, d1);
        Assertions.assertEquals("2022-06-01 16:00:00", Dates.format(d2));
    }

    @Test
    public void testComputeNextFireTimeONCE() {
        String triggerConf = "2022-06-01 10:00:00";
        Assertions.assertNull(TriggerType.ONCE.computeNextFireTime(triggerConf, Dates.toDate("2022-06-01 10:00:01")));

        Date d1 = TriggerType.ONCE.computeNextFireTime(triggerConf, Dates.toDate("2022-05-31 10:00:01"));
        Assertions.assertEquals("2022-06-01 10:00:00", Dates.format(d1));
        Assertions.assertNull(TriggerType.ONCE.computeNextFireTime(triggerConf, d1));
    }

    @Test
    public void testComputeNextFireTimePERIOD() {
        String triggerConf = "{\"period\":\"MONTHLY\", \"start\":\"2022-05-01 00:00:00\", \"step\":1}";
        Assertions.assertFalse(TriggerType.PERIOD.computeNextFireTime(triggerConf, new Date()).before(new Date()));

        Date startTime = Dates.toDate("2022-05-01 00:00:00");
        Assertions.assertEquals("2022-06-01 00:00:00", Dates.format(TriggerType.PERIOD.computeNextFireTime(triggerConf, startTime)));

        startTime = Dates.toDate("2022-05-31 23:59:58");
        Assertions.assertEquals("2022-06-01 00:00:00", Dates.format(TriggerType.PERIOD.computeNextFireTime(triggerConf, startTime)));
    }

}

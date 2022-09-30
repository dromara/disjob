package cn.ponfee.scheduler.supervisor.test.job.util;

import cn.ponfee.scheduler.common.date.Dates;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.enums.MisfireStrategy;
import cn.ponfee.scheduler.core.enums.Operations;
import cn.ponfee.scheduler.core.enums.TriggerType;
import cn.ponfee.scheduler.core.handle.JobHandler;
import cn.ponfee.scheduler.core.model.PeriodTriggerConf;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.supervisor.util.JobUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.Date;

/**
 * Test JobUtils
 *
 * @author Ponfee
 */
public class JobUtilsTest {

    @Test
    public void testInterface() {
        Assertions.assertFalse(JobHandler.class.isInterface());
        Assertions.assertTrue(Modifier.isAbstract(JobHandler.class.getModifiers()));
    }

    @Test
    public void testComputeNextFireTimeCRON_DISCARD() {
        SchedJob job = new SchedJob();
        job.setStartTime(null);
        job.setEndTime(null);
        job.setTriggerType(TriggerType.CRON.value());
        job.setTriggerConf("0 0 10,14,16 * * ?");
        job.setLastTriggerTime(null);
        job.setMisfireStrategy(MisfireStrategy.DISCARD.value());

        Date date1 = new Date(JobUtils.computeNextTriggerTime(job));
        Assertions.assertTrue(date1.after(new Date()));

        job.setLastTriggerTime(date1.getTime());
        Date date2 = new Date(JobUtils.computeNextTriggerTime(job));
        Assertions.assertTrue(date2.after(date1));
    }

    @Test
    public void testComputeNextFireTimeCRON_LAST() {
        Date now = new Date();
        SchedJob job = new SchedJob();
        job.setStartTime(null);
        job.setEndTime(null);
        job.setTriggerType(TriggerType.CRON.value());
        job.setTriggerConf("0 0 10,14,16 * * ?");
        job.setLastTriggerTime(null);
        job.setMisfireStrategy(MisfireStrategy.LAST.value());

        Date date1 = new Date(JobUtils.computeNextTriggerTime(job));
        Assertions.assertTrue(date1.after(now));
        System.out.println("date1: " + Dates.format(date1));

        job.setLastTriggerTime(date1.getTime());
        Date date2 = new Date(JobUtils.computeNextTriggerTime(job));
        Assertions.assertTrue(date2.after(date1));
        System.out.println("date2: " + Dates.format(date2));

        // test last non null
        Date last = Dates.toDate("2022-05-31 10:00:00");
        job.setLastTriggerTime(last.getTime());
        Date date3 = new Date(JobUtils.computeNextTriggerTime(job));
        Assertions.assertTrue(date3.before(now));
        System.out.println("date3: " + Dates.format(date3));

        job.setLastTriggerTime(date3.getTime());
        Date date4 = new Date(JobUtils.computeNextTriggerTime(job));
        Assertions.assertTrue(date4.after(now));
        System.out.println("date4: " + Dates.format(date4));
    }

    @Test
    public void testComputeNextFireTimeCRON_EVERY() {
        Date now = new Date();
        SchedJob job = new SchedJob();
        job.setStartTime(null);
        job.setEndTime(null);
        job.setTriggerType(TriggerType.CRON.value());
        job.setTriggerConf("0 0 1 * * ?");
        job.setLastTriggerTime(null);
        job.setMisfireStrategy(MisfireStrategy.EVERY.value());

        Date date1 = new Date(JobUtils.computeNextTriggerTime(job));
        Assertions.assertTrue(date1.after(now));
        System.out.println("date1: " + Dates.format(date1));

        job.setLastTriggerTime(date1.getTime());
        Date date2 = new Date(JobUtils.computeNextTriggerTime(job));
        Assertions.assertTrue(date2.after(date1));
        System.out.println("date2: " + Dates.format(date2));

        // test last non null
        Date last = Dates.toDate("2022-05-20 10:00:00");
        job.setLastTriggerTime(last.getTime());
        Date date3 = new Date(JobUtils.computeNextTriggerTime(job));
        Assertions.assertEquals("2022-05-21 01:00:00", Dates.format(date3));

        job.setLastTriggerTime(date3.getTime());
        Date date4 = new Date(JobUtils.computeNextTriggerTime(job));
        Assertions.assertEquals("2022-05-22 01:00:00", Dates.format(date4));

        job.setLastTriggerTime(date4.getTime());
        Date date5 = new Date(JobUtils.computeNextTriggerTime(job));
        Assertions.assertEquals("2022-05-23 01:00:00", Dates.format(date5));
    }

    @Test
    public void testComputeNextFireTimeONCE() {
        Date now = new Date();
        SchedJob job = new SchedJob();
        job.setStartTime(null);
        job.setEndTime(null);
        job.setTriggerType(TriggerType.ONCE.value());
        job.setTriggerConf("2022-05-03 00:00:00");
        job.setLastTriggerTime(null);

        // DISCARD
        job.setMisfireStrategy(MisfireStrategy.DISCARD.value());
        Assertions.assertNull(JobUtils.computeNextTriggerTime(job));

        job.setLastTriggerTime(Dates.toDate("2022-05-01 23:00:00").getTime());
        Assertions.assertNull(JobUtils.computeNextTriggerTime(job));

        job.setTriggerConf(Dates.format(Dates.plusSeconds(new Date(), 5)));
        Assertions.assertNull(JobUtils.computeNextTriggerTime(job));

        job.setLastTriggerTime(null);
        job.setTriggerConf(Dates.format(Dates.plusSeconds(new Date(), 100)));
        Assertions.assertTrue(new Date(JobUtils.computeNextTriggerTime(job)).after(new Date()));

        // EVERY
        job.setLastTriggerTime(Dates.toDate("2022-05-01 23:00:00").getTime());
        job.setTriggerConf("2022-05-03 00:00:00");
        job.setMisfireStrategy(MisfireStrategy.EVERY.value());

        //
        Assertions.assertNull(JobUtils.computeNextTriggerTime(job));

        //
        job.setLastTriggerTime(Dates.toDate("2022-05-03 00:00:00").getTime());
        Assertions.assertNull(JobUtils.computeNextTriggerTime(job));

        //
        job.setLastTriggerTime(null);
        job.setTriggerConf("2022-05-03 00:00:00");
        Assertions.assertEquals(Dates.toDate("2022-05-03 00:00:00").getTime(), JobUtils.computeNextTriggerTime(job));

        // LAST
        job.setMisfireStrategy(MisfireStrategy.LAST.value());
        job.setTriggerConf("2022-05-03 00:00:00");
        job.setLastTriggerTime(null);
        Assertions.assertEquals(Dates.toDate("2022-05-03 00:00:00").getTime(), JobUtils.computeNextTriggerTime(job));
    }

    @Test
    public void testComputeNextFireTimePERIOD() {
        Date now = new Date();
        SchedJob job = new SchedJob();
        job.setStartTime(null);
        job.setEndTime(null);
        job.setTriggerType(TriggerType.PERIOD.value());
        job.setTriggerConf("{\"period\":\"DAILY\", \"start\":\"2022-05-01 00:00:00\", \"step\":1}");
        job.setLastTriggerTime(null);

        PeriodTriggerConf triggerConf = Jsons.fromJson("{\"period\":\"DAILY\", \"start\":\"2022-05-01 00:00:00\", \"step\":1}", PeriodTriggerConf.class);

        Date tomorrow = Dates.startOfDay(Dates.plusDays(now, 1));

        // DISCARD
        job.setMisfireStrategy(MisfireStrategy.DISCARD.value());
        Assertions.assertEquals(JobUtils.computeNextTriggerTime(job), tomorrow.getTime());

        job.setLastTriggerTime(Dates.toDate("2022-05-03 23:00:00").getTime());
        Assertions.assertEquals(JobUtils.computeNextTriggerTime(job), tomorrow.getTime());

        triggerConf.setStart(tomorrow);
        job.setTriggerConf(Jsons.toJson(triggerConf));
        Assertions.assertEquals(JobUtils.computeNextTriggerTime(job), tomorrow.getTime());

        System.out.println("---------------");
        TriggerType.PERIOD.computeNextFireTimes(job.getTriggerConf(), now, 4)
            .stream()
            .forEach(e -> System.out.println(Dates.format(e)));

        // LAST
        job.setMisfireStrategy(MisfireStrategy.LAST.value());
        job.setTriggerConf("{\"period\":\"DAILY\", \"start\":\"2022-05-01 00:00:00\", \"step\":1}");
        job.setLastTriggerTime(null);
        Assertions.assertEquals(JobUtils.computeNextTriggerTime(job), tomorrow.getTime());

        job.setLastTriggerTime(Dates.toDate("2022-05-05 03:12:21").getTime());
        Assertions.assertEquals(JobUtils.computeNextTriggerTime(job), Dates.startOfDay(now).getTime());


        job.setMisfireStrategy(MisfireStrategy.EVERY.value());
        job.setTriggerConf("{\"period\":\"DAILY\", \"start\":\"2022-05-01 00:00:00\", \"step\":1}");
        job.setLastTriggerTime(null);
        Assertions.assertEquals(JobUtils.computeNextTriggerTime(job), tomorrow.getTime());

        job.setLastTriggerTime(Dates.toDate("2022-05-05 03:12:21").getTime());
        Date date = new Date(JobUtils.computeNextTriggerTime(job));
        Assertions.assertEquals(Dates.format(date), "2022-05-06 00:00:00");

        job.setLastTriggerTime(date.getTime());
        date = new Date(JobUtils.computeNextTriggerTime(job));
        Assertions.assertEquals(Dates.format(date), "2022-05-07 00:00:00");

        job.setLastTriggerTime(date.getTime());
        date = new Date(JobUtils.computeNextTriggerTime(job));
        Assertions.assertEquals(Dates.format(date), "2022-05-08 00:00:00");
    }

    @Test
    public void testTaskParam() {
        ExecuteParam param = new ExecuteParam(Operations.TRIGGER, 0, 0, 0, 0);
        Operations old = param.operation();
        Assertions.assertTrue(param.updateOperation(old, null));
        Assertions.assertNull(param.operation());
    }

}

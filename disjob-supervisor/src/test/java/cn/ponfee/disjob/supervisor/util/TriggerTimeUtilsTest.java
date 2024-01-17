/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.util;

import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.date.JavaUtilDateFormat;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.enums.*;
import cn.ponfee.disjob.core.model.PeriodTriggerValue;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * TriggerTimeUtils test
 *
 * @author Ponfee
 */
public class TriggerTimeUtilsTest {

    @Test
    public void testComputeNextTriggerTimeCRON_DISCARD() {
        SchedJob job = new SchedJob();
        job.setStartTime(null);
        job.setEndTime(null);
        job.setTriggerType(TriggerType.CRON.value());
        job.setTriggerValue("0 0 10,14,16 * * ?");
        job.setLastTriggerTime(null);
        job.setMisfireStrategy(MisfireStrategy.DISCARD.value());

        Date now = new Date();

        Date date1 = new Date(TriggerTimeUtils.computeNextTriggerTime(job, now));
        Assertions.assertTrue(date1.after(new Date()));

        job.setLastTriggerTime(date1.getTime());
        Date date2 = new Date(TriggerTimeUtils.computeNextTriggerTime(job, now));
        Assertions.assertTrue(date2.after(date1));
    }

    @Test
    public void testComputeNextTriggerTimeCRON_LAST() throws ParseException {
        Date now = new Date();
        SchedJob job = new SchedJob();
        job.setStartTime(null);
        job.setEndTime(null);
        job.setTriggerType(TriggerType.CRON.value());
        job.setTriggerValue("0 0 10,14,16 * * ?");
        job.setLastTriggerTime(null);
        job.setMisfireStrategy(MisfireStrategy.LAST.value());

        Date date1 = new Date(TriggerTimeUtils.computeNextTriggerTime(job, now));
        Assertions.assertTrue(date1.after(now));
        System.out.println("date1: " + Dates.format(date1));

        job.setLastTriggerTime(date1.getTime());
        Date date2 = new Date(TriggerTimeUtils.computeNextTriggerTime(job, now));
        Assertions.assertTrue(date2.after(date1));
        System.out.println("date2: " + Dates.format(date2));

        // test last non null
        Date last = JavaUtilDateFormat.DEFAULT.parse("2022-05-31 10:00:00");
        job.setLastTriggerTime(last.getTime());
        Date date3 = new Date(TriggerTimeUtils.computeNextTriggerTime(job, now));
        Assertions.assertTrue(date3.before(now));
        System.out.println("date3: " + Dates.format(date3));

        job.setLastTriggerTime(date3.getTime());
        Date date4 = new Date(TriggerTimeUtils.computeNextTriggerTime(job, now));
        Assertions.assertTrue(date4.after(now));
        System.out.println("date4: " + Dates.format(date4));
    }

    @Test
    public void testComputeNextTriggerTimeCRON_EVERY() throws ParseException {
        Date now = new Date();
        SchedJob job = new SchedJob();
        job.setStartTime(null);
        job.setEndTime(null);
        job.setTriggerType(TriggerType.CRON.value());
        job.setTriggerValue("0 0 1 * * ?");
        job.setLastTriggerTime(null);
        job.setMisfireStrategy(MisfireStrategy.EVERY.value());

        Date date1 = new Date(TriggerTimeUtils.computeNextTriggerTime(job, now));
        Assertions.assertTrue(date1.after(now));
        System.out.println("date1: " + Dates.format(date1));

        job.setLastTriggerTime(date1.getTime());
        Date date2 = new Date(TriggerTimeUtils.computeNextTriggerTime(job, now));
        Assertions.assertTrue(date2.after(date1));
        System.out.println("date2: " + Dates.format(date2));

        // test last non null
        Date last = JavaUtilDateFormat.DEFAULT.parse("2022-05-20 10:00:00");
        job.setLastTriggerTime(last.getTime());
        Date date3 = new Date(TriggerTimeUtils.computeNextTriggerTime(job, now));
        Assertions.assertEquals("2022-05-21 01:00:00", Dates.format(date3));

        job.setLastTriggerTime(date3.getTime());
        Date date4 = new Date(TriggerTimeUtils.computeNextTriggerTime(job, now));
        Assertions.assertEquals("2022-05-22 01:00:00", Dates.format(date4));

        job.setLastTriggerTime(date4.getTime());
        Date date5 = new Date(TriggerTimeUtils.computeNextTriggerTime(job, now));
        Assertions.assertEquals("2022-05-23 01:00:00", Dates.format(date5));
    }

    @Test
    public void testComputeNextTriggerTimeONCE() throws ParseException {
        Date max = new Date(Long.MAX_VALUE);
        Assertions.assertEquals(Long.MAX_VALUE, max.getTime());

        Date min = new Date(Long.MIN_VALUE);
        Assertions.assertEquals(Long.MIN_VALUE, min.getTime());

        Date zero = new Date(0);
        Assertions.assertEquals(0, zero.getTime());

        System.out.println();
        Date now = new Date();
        SchedJob job = new SchedJob();
        job.setStartTime(null);
        job.setEndTime(null);
        job.setTriggerType(TriggerType.ONCE.value());
        job.setTriggerValue("2022-05-03 00:00:00");
        job.setLastTriggerTime(null);

        // DISCARD
        job.setMisfireStrategy(MisfireStrategy.DISCARD.value());
        Assertions.assertNull(TriggerTimeUtils.computeNextTriggerTime(job, now));

        job.setLastTriggerTime(JavaUtilDateFormat.DEFAULT.parse("2022-05-01 23:00:00").getTime());
        Assertions.assertNull(TriggerTimeUtils.computeNextTriggerTime(job, now));

        job.setTriggerValue(Dates.format(Dates.plusSeconds(new Date(), 5)));
        Assertions.assertNull(TriggerTimeUtils.computeNextTriggerTime(job, now));

        job.setLastTriggerTime(null);
        job.setTriggerValue(Dates.format(Dates.plusSeconds(new Date(), 100)));
        Assertions.assertTrue(new Date(TriggerTimeUtils.computeNextTriggerTime(job, now)).after(new Date()));

        // EVERY
        job.setLastTriggerTime(JavaUtilDateFormat.DEFAULT.parse("2022-05-01 23:00:00").getTime());
        job.setTriggerValue("2022-05-03 00:00:00");
        job.setMisfireStrategy(MisfireStrategy.EVERY.value());

        //
        Assertions.assertNull(TriggerTimeUtils.computeNextTriggerTime(job, now));

        //
        job.setLastTriggerTime(JavaUtilDateFormat.DEFAULT.parse("2022-05-03 00:00:00").getTime());
        Assertions.assertNull(TriggerTimeUtils.computeNextTriggerTime(job, now));

        //
        job.setLastTriggerTime(null);
        job.setTriggerValue("2022-05-03 00:00:00");
        Assertions.assertEquals(JavaUtilDateFormat.DEFAULT.parse("2022-05-03 00:00:00").getTime(), TriggerTimeUtils.computeNextTriggerTime(job, now));

        // LAST
        job.setMisfireStrategy(MisfireStrategy.LAST.value());
        job.setTriggerValue("2022-05-03 00:00:00");
        job.setLastTriggerTime(null);
        Assertions.assertEquals(JavaUtilDateFormat.DEFAULT.parse("2022-05-03 00:00:00").getTime(), TriggerTimeUtils.computeNextTriggerTime(job, now));
    }

    @Test
    public void testComputeNextTriggerTimePERIOD() throws ParseException {
        Date now = new Date();
        SchedJob job = new SchedJob();
        job.setStartTime(null);
        job.setEndTime(null);
        job.setTriggerType(TriggerType.PERIOD.value());
        job.setTriggerValue("{\"period\":\"DAILY\", \"start\":\"2022-05-01 00:00:00\", \"step\":1}");
        job.setLastTriggerTime(null);

        PeriodTriggerValue triggerValue = Jsons.fromJson("{\"period\":\"DAILY\", \"start\":\"2022-05-01 00:00:00\", \"step\":1}", PeriodTriggerValue.class);

        Date tomorrow = Dates.startOfDay(Dates.plusDays(now, 1));

        // DISCARD
        job.setMisfireStrategy(MisfireStrategy.DISCARD.value());
        Assertions.assertEquals(TriggerTimeUtils.computeNextTriggerTime(job, now), tomorrow.getTime());

        job.setLastTriggerTime(JavaUtilDateFormat.DEFAULT.parse("2022-05-03 23:00:00").getTime());
        Assertions.assertEquals(TriggerTimeUtils.computeNextTriggerTime(job, now), tomorrow.getTime());

        triggerValue.setStart(tomorrow);
        job.setTriggerValue(Jsons.toJson(triggerValue));
        Assertions.assertEquals(TriggerTimeUtils.computeNextTriggerTime(job, now), tomorrow.getTime());

        System.out.println("---------------");
        List<Date> triggerTimes = TriggerType.PERIOD.computeNextTriggerTimes(job.getTriggerValue(), now, 4);
        Assertions.assertEquals(4, triggerTimes.size());
        triggerTimes.forEach(e -> System.out.println(Dates.format(e)));

        // LAST
        job.setMisfireStrategy(MisfireStrategy.LAST.value());
        job.setTriggerValue("{\"period\":\"DAILY\", \"start\":\"2022-05-01 00:00:00\", \"step\":1}");
        job.setLastTriggerTime(null);
        Assertions.assertEquals(TriggerTimeUtils.computeNextTriggerTime(job, now), tomorrow.getTime());

        job.setLastTriggerTime(JavaUtilDateFormat.DEFAULT.parse("2022-05-05 03:12:21").getTime());
        Assertions.assertEquals(TriggerTimeUtils.computeNextTriggerTime(job, now), Dates.startOfDay(now).getTime());


        job.setMisfireStrategy(MisfireStrategy.EVERY.value());
        job.setTriggerValue("{\"period\":\"DAILY\", \"start\":\"2022-05-01 00:00:00\", \"step\":1}");
        job.setLastTriggerTime(null);
        Assertions.assertEquals(TriggerTimeUtils.computeNextTriggerTime(job, now), tomorrow.getTime());

        job.setLastTriggerTime(JavaUtilDateFormat.DEFAULT.parse("2022-05-05 03:12:21").getTime());
        Date date = new Date(TriggerTimeUtils.computeNextTriggerTime(job, now));
        Assertions.assertEquals(Dates.format(date), "2022-05-06 00:00:00");

        job.setLastTriggerTime(date.getTime());
        date = new Date(TriggerTimeUtils.computeNextTriggerTime(job, now));
        Assertions.assertEquals(Dates.format(date), "2022-05-07 00:00:00");

        job.setLastTriggerTime(date.getTime());
        date = new Date(TriggerTimeUtils.computeNextTriggerTime(job, now));
        Assertions.assertEquals(Dates.format(date), "2022-05-08 00:00:00");
    }

    @Test
    public void testTaskParam() {
        ExecuteTaskParam param = CommonTest.createExecuteTaskParam(Operation.TRIGGER, 0, 0, 1L, 0, 0, JobType.GENERAL, RouteStrategy.ROUND_ROBIN, 5, "jobHandler",new Worker("default", "workerId", "host", 1));
        Operation old = param.operation();
        Assertions.assertTrue(param.updateOperation(old, null));
        Assertions.assertNull(param.operation());
    }

}

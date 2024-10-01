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

import cn.ponfee.disjob.common.date.CronExpression;
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
 * SupervisorUtils test
 *
 * @author Ponfee
 */
@SuppressWarnings("ConstantConditions")
public class TriggerTimeUtilsTest {

    @Test
    public void test_CRON_SKIP_ALL_PAST() {
        SchedJob job = new SchedJob();
        job.setStartTime(null);
        job.setEndTime(null);
        job.setTriggerType(TriggerType.CRON.value());
        job.setTriggerValue("0 0 10,14,16 * * ?");
        job.setLastTriggerTime(null);
        job.setMisfireStrategy(MisfireStrategy.SKIP_ALL_LOST.value());

        Date now = new Date();

        Date date1 = new Date(TriggerTimeUtils.computeNextTriggerTime(job, now));
        Assertions.assertTrue(date1.after(new Date()));

        job.setLastTriggerTime(date1.getTime());
        Date date2 = new Date(TriggerTimeUtils.computeNextTriggerTime(job, now));
        Assertions.assertTrue(date2.after(date1));
    }

    @Test
    public void test_CRON_FIRE_ONCE_NOW() throws ParseException {
        Date now = new Date();
        SchedJob job = new SchedJob();
        job.setStartTime(null);
        job.setEndTime(null);
        job.setTriggerType(TriggerType.CRON.value());
        job.setTriggerValue("0 0 10,14,16 * * ?");
        job.setLastTriggerTime(null);
        job.setMisfireStrategy(MisfireStrategy.FIRE_ONCE_NOW.value());

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
        Assertions.assertEquals(date3, now);
        System.out.println("date3: " + Dates.format(date3));

        job.setLastTriggerTime(date3.getTime());
        Date date4 = new Date(TriggerTimeUtils.computeNextTriggerTime(job, now));
        Assertions.assertTrue(date4.after(now));
        System.out.println("date4: " + Dates.format(date4));
    }

    @Test
    public void test_ONCE() throws ParseException {
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
        job.setMisfireStrategy(MisfireStrategy.SKIP_ALL_LOST.value());
        Assertions.assertNull(TriggerTimeUtils.computeNextTriggerTime(job, now));

        job.setLastTriggerTime(JavaUtilDateFormat.DEFAULT.parse("2022-05-01 23:00:00").getTime());
        Assertions.assertNull(TriggerTimeUtils.computeNextTriggerTime(job, now));

        job.setTriggerValue(Dates.format(Dates.plusSeconds(new Date(), 5)));
        Assertions.assertNotNull(TriggerTimeUtils.computeNextTriggerTime(job, now));

        job.setLastTriggerTime(null);
        job.setTriggerValue(Dates.format(Dates.plusSeconds(new Date(), 100)));
        Assertions.assertTrue(new Date(TriggerTimeUtils.computeNextTriggerTime(job, now)).after(new Date()));

        //
        Assertions.assertNotNull(TriggerTimeUtils.computeNextTriggerTime(job, now));

        job.setStartTime(JavaUtilDateFormat.DEFAULT.parse("2021-05-03 00:00:00"));
        job.setTriggerValue("2020-05-03 00:00:00");
        Assertions.assertNull(TriggerTimeUtils.computeNextTriggerTime(job, now));

        //
        job.setTriggerValue("2999-05-03 00:00:00");
        Assertions.assertEquals(JavaUtilDateFormat.DEFAULT.parse("2999-05-03 00:00:00").getTime(), TriggerTimeUtils.computeNextTriggerTime(job, now));

        // LAST
        job.setMisfireStrategy(MisfireStrategy.FIRE_ONCE_NOW.value());
        job.setTriggerValue("2999-05-03 00:00:00");
        Assertions.assertEquals(JavaUtilDateFormat.DEFAULT.parse("2999-05-03 00:00:00").getTime(), TriggerTimeUtils.computeNextTriggerTime(job, now));

        job.setLastTriggerTime(JavaUtilDateFormat.DEFAULT.parse("2020-05-01 23:00:00").getTime());
        job.setMisfireStrategy(MisfireStrategy.SKIP_ALL_LOST.value());
        job.setStartTime(JavaUtilDateFormat.DEFAULT.parse("2021-05-03 00:00:00"));
        job.setTriggerValue("2022-05-03 00:00:00");
        Assertions.assertNull(TriggerTimeUtils.computeNextTriggerTime(job, now));

        String triggerValue = JavaUtilDateFormat.DEFAULT.format(Dates.plusDays(now, 1));
        job.setTriggerValue(triggerValue);
        Assertions.assertEquals(TriggerTimeUtils.computeNextTriggerTime(job, now), JavaUtilDateFormat.DEFAULT.parse(triggerValue).getTime());

        job.setStartTime(Dates.plusYears(now, 1000));
        Assertions.assertNull(TriggerTimeUtils.computeNextTriggerTime(job, now));

        job.setLastTriggerTime(Dates.plusYears(now, 1000).getTime());
        Assertions.assertNull(TriggerTimeUtils.computeNextTriggerTime(job, now));
    }

    @Test
    public void test_PERIOD() throws ParseException {
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
        job.setMisfireStrategy(MisfireStrategy.SKIP_ALL_LOST.value());
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
        job.setMisfireStrategy(MisfireStrategy.FIRE_ONCE_NOW.value());
        job.setTriggerValue("{\"period\":\"DAILY\", \"start\":\"2022-05-01 00:00:00\", \"step\":1}");
        job.setLastTriggerTime(null);
        Assertions.assertEquals(TriggerTimeUtils.computeNextTriggerTime(job, now), tomorrow.getTime());

        job.setLastTriggerTime(JavaUtilDateFormat.DEFAULT.parse("2022-05-05 03:12:21").getTime());
        Long time = TriggerTimeUtils.computeNextTriggerTime(job, now);
        Assertions.assertEquals(time, now.getTime());

        Assertions.assertNotNull(TriggerType.ONCE.computeNextTriggerTime("0000-00-00 00:00:00", new Date(Long.MIN_VALUE)));
        Assertions.assertNull(TriggerType.ONCE.computeNextTriggerTime("0000-00-00 00:00:00", new Date(0)));
    }

    @Test
    public void test_PERIOD2() throws ParseException {
        SchedJob job = new SchedJob();
        job.setStartTime(null);
        job.setEndTime(null);
        job.setTriggerType(TriggerType.PERIOD.value());
        job.setTriggerValue("{\"period\":\"PER_SECOND\", \"start\":\"1000-01-01 00:00:00\", \"step\":3}");
        job.setLastTriggerTime(null);
        job.setMisfireStrategy(MisfireStrategy.SKIP_ALL_LOST.value());
        job.setLastTriggerTime(Dates.plusDays(new Date(), -1).getTime());
        System.out.println(Dates.format(TriggerTimeUtils.computeNextTriggerTime(job, new Date()), Dates.DATEFULL_PATTERN));
    }

    @Test
    public void test_CRON_FIRE_ONCE_NOW2() {
        Date now = new Date();
        SchedJob job = new SchedJob();
        job.setStartTime(null);
        job.setTriggerType(TriggerType.INTERVAL.value());
        job.setTriggerValue("60");
        job.setLastTriggerTime(Dates.minusSeconds(now, 90L).getTime());
        job.setMisfireStrategy(MisfireStrategy.FIRE_ONCE_NOW.value());
        Assertions.assertEquals(TriggerTimeUtils.computeNextTriggerTime(job, now), Dates.minusSeconds(now, 30L).getTime());
    }

    @Test
    public void testCRON() throws ParseException {
        String expr = Dates.toCronExpression(parse("2022-10-06 22:55:00.000"));
        System.out.println(expr);
        System.out.println(format(new CronExpression(expr).getNextValidTimeAfter(new Date())));

        SchedJob job = new SchedJob();
        job.setTriggerType(TriggerType.CRON.value());
        job.setTriggerValue(expr);
        job.setMisfireStrategy(MisfireStrategy.SKIP_ALL_LOST.value());
        Assertions.assertNull(TriggerTimeUtils.computeNextTriggerTime(job, new Date()));

        job.setMisfireStrategy(MisfireStrategy.FIRE_ALL_LOST.value());
        job.setStartTime(parse("2022-01-01 00:00:00.000"));
        Assertions.assertEquals("2022-10-06 22:55:00.000", format(Dates.ofTimeMillis(TriggerTimeUtils.computeNextTriggerTime(job, new Date()))));

        job.setMisfireStrategy(MisfireStrategy.FIRE_ONCE_NOW.value());
        Assertions.assertEquals("2022-10-06 22:55:00.000", format(Dates.ofTimeMillis(TriggerTimeUtils.computeNextTriggerTime(job, new Date()))));
    }

    @Test
    public void testComputeFirstTriggerTime() {
        String s1 = "2022-01-01 00:00:00.000", s2 = "2022-01-01 00:00:00.001";
        Date d1 = parse(s1), d2 = Dates.plusMillis(d1, 1);

        Date date;

        date = TriggerType.CRON.computeFirstTriggerTime("0 0 0 * * ?", d1);
        Assertions.assertEquals(s1, format(date));

        date = TriggerType.CRON.computeFirstTriggerTime("0 0 0 * * ?", d2);
        Assertions.assertEquals("2022-01-02 00:00:00.000", format(date));

        date = TriggerType.PERIOD.computeFirstTriggerTime("{\"period\":\"MONTHLY\", \"start\":\"2022-01-01 00:00:00\", \"step\":3}", d1);
        Assertions.assertEquals(s1, format(date));

        date = TriggerType.PERIOD.computeFirstTriggerTime("{\"period\":\"MONTHLY\", \"start\":\"2022-01-01 00:00:00\", \"step\":3}", d2);
        Assertions.assertEquals("2022-04-01 00:00:00.000", format(date));

        date = TriggerType.ONCE.computeFirstTriggerTime("2022-01-01 00:00:00", d1);
        Assertions.assertEquals(s1, format(date));

        date = TriggerType.ONCE.computeFirstTriggerTime("2022-01-01 00:00:00", d2);
        Assertions.assertNull(date);

        date = TriggerType.INTERVAL.computeFirstTriggerTime("60", d1);
        Assertions.assertEquals(s1, format(date));

        date = TriggerType.INTERVAL.computeFirstTriggerTime("60", d2);
        Assertions.assertEquals(s2, format(date));

        date = TriggerType.FIXED_RATE.computeFirstTriggerTime("60", d1);
        Assertions.assertEquals(s1, format(date));

        date = TriggerType.FIXED_RATE.computeFirstTriggerTime("60", d2);
        Assertions.assertEquals(s2, format(date));

        date = TriggerType.FIXED_DELAY.computeFirstTriggerTime("60", d1);
        Assertions.assertEquals(s1, format(date));

        date = TriggerType.FIXED_DELAY.computeFirstTriggerTime("60", d2);
        Assertions.assertEquals(s2, format(date));
    }

    @Test
    public void testTaskParam() {
        ExecuteTaskParam param = CommonTest.createExecuteTaskParam(Operation.TRIGGER, 0, 0, 1L, 0, 0, JobType.GENERAL, RouteStrategy.ROUND_ROBIN, ShutdownStrategy.RESUME, 5, "jobExecutor", new Worker("default", "workerId", "host", 1));
        Assertions.assertSame(param.getOperation(), Operation.TRIGGER);
    }

    private static Date parse(String string) {
        return Dates.toDate(string, Dates.DATEFULL_PATTERN);
    }

    private static String format(Date date) {
        return Dates.format(date, Dates.DATEFULL_PATTERN);
    }

}

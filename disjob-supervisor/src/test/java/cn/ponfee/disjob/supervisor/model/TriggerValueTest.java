/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.model;

import cn.ponfee.disjob.common.date.DatePeriods;
import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.model.PeriodTriggerValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author Ponfee
 */
public class TriggerValueTest {

    @Test
    public void testValue() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> DatePeriods.valueOf("ABC"));

        String conf = "{\"period\":\"DAILY\", \"start\":\"2000-01-01 00:00:00\", \"step\":2}";
        PeriodTriggerValue triggerValue = Jsons.fromJson(conf, PeriodTriggerValue.class);
        Assertions.assertEquals(triggerValue.getPeriod(), DatePeriods.DAILY);
        Assertions.assertEquals(triggerValue.getStep(), 2);
        Assertions.assertEquals(triggerValue.getStart(), Dates.toDate("2000-01-01 00:00:00"));
    }

    @Test
    public void testInt() {
        // 30天的毫秒书转int会溢出，要使用long类型
        int num = 30 * 86400 * 1000;
        Assertions.assertTrue(num < 0);
        Assertions.assertEquals(365, TimeUnit.DAYS.toMillis(365) / (1000 * 86400));
    }

}

/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.util;

import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Ponfee
 */
public class ScheduledExecutorTest {

    @Test
    public void testSubmit() throws InterruptedException {
        ScheduledThreadPoolExecutor registryScheduledExecutor = new ScheduledThreadPoolExecutor(1, ThreadPoolExecutors.DISCARD);
        registryScheduledExecutor.scheduleWithFixedDelay(() -> System.out.println("scheduled"), 500, 200, TimeUnit.MILLISECONDS);


        System.out.println(new Date());
        registryScheduledExecutor.submit(() -> System.out.println("submit"));
        registryScheduledExecutor.execute(() -> System.out.println("execute"));
        System.out.println(new Date());

        Thread.sleep(1000);
    }
}

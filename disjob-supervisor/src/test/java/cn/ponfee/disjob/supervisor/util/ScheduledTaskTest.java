/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.util;

import cn.ponfee.disjob.common.date.Dates;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Ponfee
 */
public class ScheduledTaskTest {

    private static final AtomicInteger count = new AtomicInteger(0);
    private static final ScheduledThreadPoolExecutor SCHEDULED_TASK = new ScheduledThreadPoolExecutor(
        1, r -> {
        Thread t = new Thread(Thread.currentThread().getThreadGroup(), r, "sc-task");
        t.setDaemon(true);
        return t;
    });

    public static void main(String[] args) throws InterruptedException {
        test1();
        //test2();
    }

    public static void test1() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        SCHEDULED_TASK.scheduleWithFixedDelay(() -> {
            int cnt = count.get();
            System.out.println(cnt + " -> " + Dates.format(new Date()));
            count.incrementAndGet();
            if (cnt == 5) {
                throw new IllegalArgumentException("my exception");
            }
        }, 0, 1, TimeUnit.SECONDS);
        latch.await();
    }

    public static void test2() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        SCHEDULED_TASK.scheduleWithFixedDelay(() -> {
            try {
                int cnt = count.get();
                System.out.println(cnt + " -> " + Dates.format(new Date()));
                count.incrementAndGet();
                if (cnt == 5) {
                    throw new IllegalArgumentException("my exception");
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
        latch.await();
    }

}

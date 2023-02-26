/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.test.job.thread;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Ponfee
 */
public class ThreadExceptionTest {

    @Test
    public void test() throws InterruptedException {
        Thread t = new Thread(() -> System.out.println(1 / 0));
        //自定义未捕获异常
        t.setUncaughtExceptionHandler((thread, throwable) -> {
            System.out.println(thread);
            System.out.println(thread == t);
            System.out.println(throwable.getMessage());
        });
        t.start();

        System.out.println("ThreadExceptionTest#test join start: " + t);
        t.join();
        System.out.println("ThreadExceptionTest#test join end: " + t);
    }

    @Test
    public void testQueueOfferSync() throws InterruptedException {
        SynchronousQueue queue = new SynchronousQueue();
        if (!queue.offer(new Object(), 1, TimeUnit.SECONDS)) {
            System.out.println(123);
        }
    }

    @Test
    public void testQueueOfferAsync() throws InterruptedException {
        SynchronousQueue queue = new SynchronousQueue();

        Thread t = new Thread(() -> {
            try {
                if (!queue.offer(new Object(), 1, TimeUnit.SECONDS)) {
                    System.out.println(123);
                }
            } catch (InterruptedException e) {
                System.out.println("InterruptedException " + e.getMessage());
            }
        });

        System.out.println("start---");
        t.start();
        t.interrupt();
        //Thread.interrupted();
        Thread.sleep(2000);
        System.out.println("end---");
    }

    @Test
    public void testQueuePollSync() throws InterruptedException {
        long nanosecondsPerSeconds = TimeUnit.SECONDS.toNanos(1);
        Assertions.assertEquals(1000_000_000, nanosecondsPerSeconds);
        SynchronousQueue queue = new SynchronousQueue();
        long startTime = System.nanoTime();
        Assertions.assertNull(queue.poll(nanosecondsPerSeconds, TimeUnit.NANOSECONDS));
        System.out.println(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime - nanosecondsPerSeconds));
    }
}

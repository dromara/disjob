/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.base;

import cn.ponfee.disjob.common.concurrent.Threads;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Ponfee
 */
public class RateLimiterTest {

    @Test
    public void testJoin() throws InterruptedException {
        Thread thread = new Thread();
        Assertions.assertTrue(thread.getState() == Thread.State.NEW);
        thread.join();


        thread = new Thread();
        thread.start();
        Thread.sleep(100);
        Assertions.assertTrue(Threads.isStopped(thread));
        thread.join();
    }

    @Test
    public void testStop() throws InterruptedException {
        MyThread1 thread = new MyThread1();
        thread.start();
        Thread.sleep(200);
        Assertions.assertFalse(Threads.isStopped(thread));
        Threads.stopThread(thread, 500);
        Thread.sleep(100);
        Assertions.assertTrue(Threads.isStopped(thread));
        thread.join();

        System.out.println("\n\n------------------");
        Thread t = new Thread(){
            @Override
            public void run() {
                ThrowingRunnable.doCaught(() -> Thread.sleep(20));
                Threads.stopThread(this, 0);
                ThrowingRunnable.doCaught(() -> Thread.sleep(20));
            }
        };
        t.start();
        Thread.sleep(300);
    }

    @Test
    public void testInterrupt() throws InterruptedException {
        MyThread2 thread = new MyThread2();
        thread.start();
        Thread.sleep(300);
        Assertions.assertFalse(Threads.isStopped(thread));
        thread.interrupt();
        Thread.sleep(200);
        Assertions.assertFalse(Threads.isStopped(thread));
        // RateLimiter不会interrupt
    }

    public static class MyThread1 extends Thread {
        RateLimiter rateLimiter = RateLimiter.create(3, Duration.ofSeconds(10));

        @Override
        public void run() {
            while (true) {
                System.out.println(rateLimiter.acquire(ThreadLocalRandom.current().nextInt(5) + 1));
            }
        }
    }

    public static class MyThread2 extends Thread {
        RateLimiter rateLimiter = RateLimiter.create(3, Duration.ofSeconds(10));

        @Override
        public void run() {
            while (true) {
                System.out.println(rateLimiter.acquire(ThreadLocalRandom.current().nextInt(5) + 1));
            }
        }
    }

}

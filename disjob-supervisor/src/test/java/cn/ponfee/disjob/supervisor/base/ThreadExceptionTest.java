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

package cn.ponfee.disjob.supervisor.base;

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
        Thread.sleep(500);
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

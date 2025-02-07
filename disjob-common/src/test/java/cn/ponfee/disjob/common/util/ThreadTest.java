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

package cn.ponfee.disjob.common.util;

import cn.ponfee.disjob.common.concurrent.NamedThreadFactory;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.concurrent.Threads;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;

/**
 * Thread Test
 *
 * @author Ponfee
 */
class ThreadTest {
    private static final Logger log = LoggerFactory.getLogger(ThreadTest.class);

    @SuppressWarnings("ConstantConditions")
    @Test
    void testGetStackFrame() {
        Assertions.assertTrue(Threads.getStackFrame(0).startsWith("java.lang.Thread.getStackTrace("));
        Assertions.assertTrue(Threads.getStackFrame(1).startsWith("cn.ponfee.disjob.common.concurrent.Threads.getStackFrame("));
        Assertions.assertTrue(Threads.getStackFrame(2).startsWith("cn.ponfee.disjob.common.util.ThreadTest.testGetStackFrame("));
    }

    @Test
    void testMaximumPoolSize__CALLER_RUNS() {
        ExecutorService threadPool = ThreadPoolExecutors.builder()
            .corePoolSize(1)
            .maximumPoolSize(4)
            .workQueue(new SynchronousQueue<>())
            .keepAliveTimeSeconds(1)
            .rejectedHandler(ThreadPoolExecutors.CALLER_RUNS)
            .threadFactory(NamedThreadFactory.builder().prefix("notify_server").uncaughtExceptionHandler(log).build())
            .build();

        for (int i = 0; i < 15; i++) {
            final int x = i;
            threadPool.submit(() -> {
                try {
                    Thread.sleep("main".equals(Thread.currentThread().getName()) ? 2000 : 200);
                    System.out.println("----------" + Thread.currentThread().getName() + ", " + x);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        ThreadPoolExecutors.shutdown(threadPool);
    }

    @Test
    void testMaximumPoolSize__CALLER_BLOCKS() {
        ExecutorService threadPool = ThreadPoolExecutors.builder()
            .corePoolSize(1)
            .maximumPoolSize(4)
            .workQueue(new SynchronousQueue<>())
            .keepAliveTimeSeconds(1)
            .rejectedHandler(ThreadPoolExecutors.CALLER_BLOCKS)
            .threadFactory(NamedThreadFactory.builder().prefix("notify_server").uncaughtExceptionHandler(log).build())
            .build();

        for (int i = 0; i < 15; i++) {
            final int x = i;
            threadPool.submit(() -> {
                try {
                    Thread.sleep("main".equals(Thread.currentThread().getName()) ? 2000 : 200);
                    System.out.println("----------" + Thread.currentThread().getName() + ", " + x);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        ThreadPoolExecutors.shutdown(threadPool);
    }

    @Test
    void testKeepAliveTimeSeconds__CALLER_RUNS() {
        ExecutorService threadPool = ThreadPoolExecutors.builder()
            .corePoolSize(1)
            .maximumPoolSize(4)
            .workQueue(new SynchronousQueue<>())
            .keepAliveTimeSeconds(0)
            .rejectedHandler(ThreadPoolExecutors.CALLER_RUNS)
            .allowCoreThreadTimeOut(false)
            .threadFactory(NamedThreadFactory.builder().prefix("notify_server").uncaughtExceptionHandler(log).build())
            .build();

        for (int i = 0; i < 15; i++) {
            final int x = i;
            threadPool.submit(() -> {
                try {
                    Thread.sleep("main".equals(Thread.currentThread().getName()) ? 2000 : 200);
                    System.out.println("----------" + Thread.currentThread().getName() + ", " + x);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        ThreadPoolExecutors.shutdown(threadPool);
    }

    @Test
    void testKeepAliveTimeSeconds__CALLER_BLOCKS() {
        ExecutorService threadPool = ThreadPoolExecutors.builder()
            .corePoolSize(1)
            .maximumPoolSize(4)
            .workQueue(new SynchronousQueue<>())
            .keepAliveTimeSeconds(0)
            .rejectedHandler(ThreadPoolExecutors.CALLER_BLOCKS)
            .allowCoreThreadTimeOut(false)
            .threadFactory(NamedThreadFactory.builder().prefix("notify_server").uncaughtExceptionHandler(log).build())
            .build();

        for (int i = 0; i < 15; i++) {
            final int x = i;
            threadPool.submit(() -> {
                try {
                    Thread.sleep("main".equals(Thread.currentThread().getName()) ? 2000 : 200);
                    System.out.println("----------" + Thread.currentThread().getName() + ", " + x);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        ThreadPoolExecutors.shutdown(threadPool);
    }

}

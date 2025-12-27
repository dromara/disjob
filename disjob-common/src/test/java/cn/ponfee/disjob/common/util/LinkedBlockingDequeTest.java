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

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongFunction;

/**
 * LinkedBlockingDequeTest
 *
 * @author Ponfee
 */
class LinkedBlockingDequeTest {

    private static final int THREAD_NUMBER = 10;
    private static final long ROUND = 1000000L;
    private static final LinkedBlockingDeque<Long> QUEUE = new LinkedBlockingDeque<>();

    static {
        for (long i = 0; i <= ROUND; i++) {
            QUEUE.add(i);
        }
    }

    @Test
    void testFor() throws InterruptedException {
        findInQueue(target -> {
            for (Long l : QUEUE) {
                if (l == target) {
                    return true;
                }
            }
            return false;
        });
    }

    @Test
    void testIterator() throws InterruptedException {
        findInQueue(target -> {
            for (Iterator<Long> iter = QUEUE.iterator(); iter.hasNext(); ) {
                if (iter.next() == target) {
                    return true;
                }
            }
            return false;
        });
    }

    @Test
    void testStream() throws InterruptedException {
        findInQueue(target -> QUEUE.stream().anyMatch(x -> x == target));
    }

    private void findInQueue(LongFunction<Boolean> function) throws InterruptedException {
        Thread[] threads = new Thread[THREAD_NUMBER];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                long start = System.currentTimeMillis();
                long target = ROUND + 5 - ThreadLocalRandom.current().nextInt(10);
                boolean found = function.apply(target);
                System.out.println("find cost time: " + Thread.currentThread().getId() + "," + (System.currentTimeMillis() - start) + ", " + found + ", " + target);
            });
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }

}

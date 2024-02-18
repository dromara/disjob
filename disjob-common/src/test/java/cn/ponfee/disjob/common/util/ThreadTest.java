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

import cn.ponfee.disjob.common.concurrent.LoggedUncaughtExceptionHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread Test
 *
 * @author Ponfee
 */
class ThreadTest {
    private static final Logger log = LoggerFactory.getLogger(ThreadTest.class);

    @Test
    void testThreadDeath() throws InterruptedException {
        Thread t = new Thread(() -> {
            for (int i = 0; ; i++) {
                System.out.print(i + " ");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    System.out.println("\n\nInterruptedException\n");
                } catch (ThreadDeath e) {
                    System.out.println("\n\nThreadDeath: " + i + "\n");
                    if (i > 10) {
                        System.out.println("i > 10");
                        throw e;
                        //break;
                    }
                }
            }
        });

        t.setUncaughtExceptionHandler(new LoggedUncaughtExceptionHandler(log));

        Assertions.assertFalse(t.isAlive());
        t.start();
        Assertions.assertTrue(t.isAlive());
        Thread.sleep(500);
        Assertions.assertTrue(t.isAlive());
        t.stop();
        Assertions.assertTrue(t.isAlive());
        Thread.sleep(1000);
        Assertions.assertTrue(t.isAlive());
        t.stop();
        Thread.sleep(500);
        Assertions.assertFalse(t.isAlive());
        t.join();
        Assertions.assertFalse(t.isAlive());
    }

}

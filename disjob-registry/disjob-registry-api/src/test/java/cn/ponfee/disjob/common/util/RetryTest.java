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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Retry test
 *
 * @author Ponfee
 */
public class RetryTest {

    static int retryMaxCount = 3;

    @Test
    public void test1() {
        int serverNumber = 1;
        int start = ThreadLocalRandom.current().nextInt(serverNumber);
        // minimum retry two times
        for (int i = 0, n = Math.min(serverNumber, retryMaxCount); i <= n; i++) {
            System.out.print(n + ": " + i + ", " + ((start + i) % serverNumber));
            if (i < n) {
                // round-robin retry
                System.out.print(", ");
                System.out.print(serverNumber == 1 ? 500 : 100L * (i + 1));
                Assertions.assertEquals(500, serverNumber == 1 ? 500 : 100L * (i + 1));
            }
            System.out.println();
        }
    }

    @Test
    public void testN() {
        int serverNumber = 10;
        int start = ThreadLocalRandom.current().nextInt(serverNumber);
        // minimum retry two times
        for (int i = 0, n = Math.min(serverNumber, retryMaxCount); i <= n; i++) {
            System.out.print(n + ": " + i + ", " + ((start + i) % serverNumber));
            if (i < n) {
                // round-robin retry
                System.out.print(", ");
                System.out.print(serverNumber == 1 ? 500 : 100L * (i + 1));
                Assertions.assertEquals(100L * (i + 1), serverNumber == 1 ? 500 : 100L * (i + 1));
            }
            System.out.println();
        }
    }

}

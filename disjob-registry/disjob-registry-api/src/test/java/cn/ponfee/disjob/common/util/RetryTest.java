/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.util;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Retry test
 *
 * @author Ponfee
 */
public class RetryTest {

    static int maxRetryTimes = 3;

    @Test
    public void test1() {
        int serverNumber = 1;
        int start = ThreadLocalRandom.current().nextInt(serverNumber);
        // minimum retry two times
        for (int i = 0, n = Math.min(serverNumber, maxRetryTimes); i <= n; i++) {
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
        for (int i = 0, n = Math.min(serverNumber, maxRetryTimes); i <= n; i++) {
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

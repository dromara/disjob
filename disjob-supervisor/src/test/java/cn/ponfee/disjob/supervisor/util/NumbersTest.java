/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.util;

import cn.ponfee.disjob.common.util.Numbers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;

/**
 * Test the old JUnit Vintage framework
 *
 * @author Ponfee
 */
public class NumbersTest {

    @Test
    public void testFormat() {
        Assertions.assertEquals("3.142", Numbers.format(Math.PI));
        Assertions.assertEquals("314.16%", Numbers.percent(Math.PI, 2));

        int i = 100;
        try {
            i = 1 / 0;
        } catch (Exception e) {
        }
        Assertions.assertEquals(100, i);
    }

    @Test
    public void testRandom() {
        double min = 1.0D, max = 0.0D;
        Random random = new Random();
        for (int i = 0; i < 1000000; i++) {
            double r = random.nextDouble();
            if (r < min) {
                min = r;
            }
            if (r > max) {
                max = r;
            }
        }

        System.out.printf("Random min=%s, max=%s%n", Numbers.format(min, "#,##0.000000000"), Numbers.format(max, "#,##0.000000000"));
        System.out.println(Numbers.format(min + max, "#,##0.000000000"));
    }

}

package cn.ponfee.scheduler.supervisor.test.common.util;

import cn.ponfee.scheduler.common.util.Numbers;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

/**
 * Test the old JUnit Vintage framework
 *
 * @author Ponfee
 */
public class NumbersTest {

    @Test
    public void testFormat() {
        Assert.assertEquals("3.142", Numbers.format(Math.PI));
        Assert.assertEquals("314.16%", Numbers.percent(Math.PI, 2));

        int i = 100;
        try {
            i = 1 / 0;
        } catch (Exception e) {
        }
        Assert.assertEquals(100, i);
    }

    @Test
    public void testRandom() {
        double min = 1.0D, max = 0.0D;
        Random random = new Random();
        for (int i = 0; i < 100000000; i++) {
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

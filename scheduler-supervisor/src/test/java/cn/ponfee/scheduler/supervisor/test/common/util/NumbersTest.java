package cn.ponfee.scheduler.supervisor.test.common.util;

import cn.ponfee.scheduler.common.util.Numbers;
import org.junit.Assert;
import org.junit.Test;

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

}

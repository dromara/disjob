/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.samples;

import cn.ponfee.scheduler.samples.common.util.Prime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;

/**
 * @author Ponfee
 */
public class PrimeTest {

    private static final long NUMBER = 5000000L;

    @Test
    public void test() {
        int n = new Random().nextInt(1000000) + 1;
        long count1 = Prime.Power.countPrimes(2, n);
        long count2 = Prime.Sqrt.countPrimes(2, n);
        long count3 = Prime.EratosthenesSieve.countPrimes(n);
        long count4 = Prime.MillerRabin.countPrimes(2, n);
        long count5 = Prime.EulerSieve.countPrimes(n);
        System.out.println(n + "  -->  " + count1);
        Assertions.assertEquals(count1, count2);
        //Assertions.assertEquals(count1, count3);
        Assertions.assertEquals(count1, count4);
        Assertions.assertEquals(count1, count5);
    }

    @Test
    public void testPower() {
        System.out.println(Prime.Power.countPrimes(2, NUMBER));
        //System.out.println(Power.countPrimes(number * 1000, number * 1001));
    }

    @Test
    public void testSqrt() {
        System.out.println(Prime.Sqrt.countPrimes(2, NUMBER));
        //System.out.println(Sqrt.countPrimes(number * 1000, number * 1001));
    }

    @Test
    public void testEratosthenesSieve() {
        System.out.println(Prime.EratosthenesSieve.countPrimes((int) NUMBER));
    }

    @Test
    public void testEulerSieve() {
        System.out.println(Prime.EulerSieve.countPrimes((int) NUMBER));
    }

    @Test
    public void testMillerRabin() {
        System.out.println(Prime.MillerRabin.countPrimes(2, NUMBER));
    }

}

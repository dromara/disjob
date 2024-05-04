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

package cn.ponfee.disjob.test.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Ponfee
 */
public class PrimeTest {

    private static final long NUMBER = 10000000L;

    @Test
    public void test0() {
        for (int i = 0; i <= 127; i++) {
            assertPrime(i);
        }

        int randomNumber = ThreadLocalRandom.current().nextInt(1000000);

        for (int i = 0; i < 10; i++) {
            assertPrime(ThreadLocalRandom.current().nextInt(randomNumber));
        }

        for (int i = 0; i < 10; i++) {
            int m = ThreadLocalRandom.current().nextInt(randomNumber);
            int n = m + ThreadLocalRandom.current().nextInt(randomNumber);
            long count1 = Prime.Power.countPrimes(m, n);
            long count2 = Prime.Sqrt.countPrimes(m, n);
            long count3 = Prime.MillerRabin.countPrimes(m, n);
            Assertions.assertEquals(count1, count2);
            Assertions.assertEquals(count1, count3);
        }
    }

    @Test
    public void testPower() {
        System.out.println(Prime.Power.countPrimes(0, NUMBER));
    }

    @Test
    public void testSqrt() {
        System.out.println(Prime.Sqrt.countPrimes(0, NUMBER));
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

    private static void assertPrime(int n) {
        long count1 = Prime.Power.countPrimes(0, n);
        long count2 = Prime.Sqrt.countPrimes(0, n);
        long count3 = Prime.EratosthenesSieve.countPrimes(n);
        long count4 = Prime.MillerRabin.countPrimes(0, n);
        long count5 = Prime.EulerSieve.countPrimes(n);
        Assertions.assertEquals(count1, count2);
        Assertions.assertEquals(count1, count3);
        Assertions.assertEquals(count1, count4);
        Assertions.assertEquals(count1, count5);
    }

}

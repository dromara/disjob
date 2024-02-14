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

import cn.ponfee.disjob.common.util.Jsons;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
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
        //Assertions.assertEquals(count1, count5);
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

    @Test
    public void test1() {
        String script = "#!/bin/sh\necho \"hello shell!\"";
        System.out.println("--------------------");
        System.out.println(script);
        System.out.println("--------------------\n");
        script = StringUtils.replaceEach(script, new String[]{"\r", "\n", "\""}, new String[]{"\\r", "\\n", "\\\""});
        Map<String, String> map = ImmutableMap.of(
            "type", "SHELL",
            "script", script
        );
        System.out.println(Jsons.toJson(map));
    }

}

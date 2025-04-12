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

import java.util.Arrays;
import java.util.Random;

/**
 * Numbers test
 *
 * @author Ponfee
 */
public class NumbersTest {

    @Test
    public void testProrate() {
        Assertions.assertEquals("[95, 3, 102]", Arrays.toString(Numbers.prorate(new int[]{43, 1, 47}, 200)));
        Assertions.assertEquals("[29, 1, 31]", Arrays.toString(Numbers.prorate(new int[]{43, 1, 47}, 61)));
        Assertions.assertEquals("[249, 249, 248, 2]", Arrays.toString(Numbers.prorate(new int[]{249, 249, 249, 3}, 748)));
    }

    @Test
    public void testPartition() {
        Assertions.assertEquals("[(0, 0)]", Numbers.partition(0, 2).toString());
        Assertions.assertEquals("[(0, 0), (1, 1)]", Numbers.partition(2, 3).toString());
        Assertions.assertEquals("[(0, 2)]", Numbers.partition(3, 1).toString());
        Assertions.assertEquals("[(0, 3), (4, 7), (8, 10)]", Numbers.partition(11, 3).toString());
    }

    @Test
    public void testSlice() {
        Assertions.assertEquals("[4, 4, 3]", Arrays.toString(Numbers.slice(11, 3)));
    }


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
        for (int i = 0; i < 1000; i++) {
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

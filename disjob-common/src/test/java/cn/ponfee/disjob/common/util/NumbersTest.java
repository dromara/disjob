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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.LongStream;

/**
 * Numbers test
 *
 * @author Ponfee
 */
public class NumbersTest {

    @Test
    public void testPercent() {
        Assertions.assertEquals("-", (Numbers.percent(1.0, 0.0, 2)));
        Assertions.assertEquals("-", Numbers.percent(0.0, 0.0, 2));
        Assertions.assertEquals("0.00%", Numbers.percent(0.0, 1.0, 2));
        Assertions.assertEquals("50.00%", Numbers.percent(1.0, 2.0, 2));
    }

    @Test
    public void testProrate() {
        Assertions.assertEquals("[123]", Arrays.toString(LongStream.rangeClosed(0, 0).map(x -> 123).toArray()));
        Assertions.assertEquals("[29, 1, 31]", Arrays.toString(Numbers.prorate(new long[]{43, 1, 47}, 61)));
        Assertions.assertEquals("[249, 249, 248, 2]", Arrays.toString(Numbers.prorate(new long[]{249, 249, 249, 3}, 748)));
        Assertions.assertEquals("[-7, -4, -4, -2, -1, 0]", Arrays.toString(Numbers.prorate(new long[]{-75, -47, -42, -24, -7, -15}, -18)));

        // 正数
        for (int i = 0; i < 100; i++) {
            long[] array = LongStream.rangeClosed(0, ThreadLocalRandom.current().nextInt(47))
                .map(e -> ThreadLocalRandom.current().nextLong(97))
                .toArray();
            long total = ThreadLocalRandom.current().nextLong(sum(array) + 1);
            Assertions.assertEquals(total, sum(Numbers.prorate(array, total)));
        }

        // 负数
        for (int i = 0; i < 100; i++) {
            long[] array = LongStream.rangeClosed(0, ThreadLocalRandom.current().nextInt(47))
                .map(e -> -1 * ThreadLocalRandom.current().nextLong(97))
                .toArray();
            long total = -1 * ThreadLocalRandom.current().nextLong(-1 * sum(array) + 1);
            Assertions.assertEquals(total, sum(Numbers.prorate(array, total)));
        }

        // 异常
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.prorate(null, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.prorate(new long[]{}, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.prorate(new long[]{1, 2, 3}, -1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.prorate(new long[]{1, 2, 3}, 7));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.prorate(new long[]{-1, -2, -3}, 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.prorate(new long[]{-1, -2, -3}, -7));
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
        for (int i = 0; i < 100; i++) {
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

    @Test
    public void testCeilDiv() {
        Assertions.assertEquals(0, (long) -0.8D);
        Assertions.assertEquals(0, (long) 0.8D);
        Assertions.assertEquals(0, (long) Math.ceil(-0.8D));
        Assertions.assertEquals(1, (long) Math.ceil(0.8D));

        Assertions.assertEquals(3, Maths.ceilDiv(7, 3));
        Assertions.assertEquals(-2, Maths.ceilDiv(-7, 3));
        Assertions.assertEquals(-2, Maths.ceilDiv(7, -3));
        Assertions.assertEquals(3, Maths.ceilDiv(-7, -3));

        Assertions.assertEquals(2, Maths.ceilDiv(10, 5));
        Assertions.assertEquals(1, Maths.ceilDiv(1, 1));
        Assertions.assertEquals(0, Maths.ceilDiv(-1, 2));
        Assertions.assertEquals(0, Maths.ceilDiv(1, -2));

        Assertions.assertEquals(0, Maths.ceilDiv(0, 5));
        Assertions.assertEquals(0, Maths.ceilDiv(0, -5));
        Assertions.assertEquals(-5, Maths.ceilDiv(5, -1));
        Assertions.assertEquals(-5, Maths.ceilDiv(-5, 1));

        Assertions.assertEquals(1, Maths.ceilDiv(1, 3));
        Assertions.assertEquals(0, Maths.ceilDiv(-1, 3));
        Assertions.assertEquals(1, Maths.ceilDiv(2, 3));
        Assertions.assertEquals(0, Maths.ceilDiv(-2, 3));
    }

    @Test
    public void testUpDiv() {
        Assertions.assertEquals(3, Maths.upDiv(7, 3));
        Assertions.assertEquals(-3, Maths.upDiv(-7, 3));
        Assertions.assertEquals(-3, Maths.upDiv(7, -3));
        Assertions.assertEquals(3, Maths.upDiv(-7, -3));

        Assertions.assertEquals(2, Maths.upDiv(10, 5));
        Assertions.assertEquals(1, Maths.upDiv(1, 1));
        Assertions.assertEquals(-1, Maths.upDiv(-1, 2));
        Assertions.assertEquals(-1, Maths.upDiv(1, -2));

        Assertions.assertEquals(0, Maths.upDiv(0, 5));
        Assertions.assertEquals(0, Maths.upDiv(0, -5));
        Assertions.assertEquals(-5, Maths.upDiv(5, -1));
        Assertions.assertEquals(-5, Maths.upDiv(-5, 1));

        Assertions.assertEquals(1, Maths.upDiv(1, 3));
        Assertions.assertEquals(-1, Maths.upDiv(-1, 3));
        Assertions.assertEquals(1, Maths.upDiv(2, 3));
        Assertions.assertEquals(-1, Maths.upDiv(-2, 3));
    }

    @Test
    public void testUpDiv2() {
        Assertions.assertEquals(3, upDiv2(7, 3));
        Assertions.assertEquals(-3, upDiv2(-7, 3));
        Assertions.assertEquals(-3, upDiv2(7, -3));
        Assertions.assertEquals(3, upDiv2(-7, -3));

        Assertions.assertEquals(2, upDiv2(10, 5));
        Assertions.assertEquals(1, upDiv2(1, 1));
        Assertions.assertEquals(-1, upDiv2(-1, 2));
        Assertions.assertEquals(-1, upDiv2(1, -2));

        Assertions.assertEquals(0, upDiv2(0, 5));
        Assertions.assertEquals(0, upDiv2(0, -5));
        Assertions.assertEquals(-5, upDiv2(5, -1));
        Assertions.assertEquals(-5, upDiv2(-5, 1));

        Assertions.assertEquals(1, upDiv2(1, 3));
        Assertions.assertEquals(-1, upDiv2(-1, 3));
        Assertions.assertEquals(1, upDiv2(2, 3));
        Assertions.assertEquals(-1, upDiv2(-2, 3));

        for (long i = 0; i < 100; i++) {
            long x = ThreadLocalRandom.current().nextLong(-1000, 1001);
            long y = ThreadLocalRandom.current().nextLong(-1000, 1001);
            if (y != 0) {
                Assertions.assertEquals(Maths.upDiv(x, y), upDiv2(x, y));
            }
        }
    }

    @Test
    public void testDownDiv() {
        Assertions.assertEquals(2, Maths.downDiv(7, 3));
        Assertions.assertEquals(-2, Maths.downDiv(-7, 3));
        Assertions.assertEquals(-2, Maths.downDiv(7, -3));
        Assertions.assertEquals(2, Maths.downDiv(-7, -3));

        Assertions.assertEquals(2, Maths.downDiv(10, 5));
        Assertions.assertEquals(1, Maths.downDiv(1, 1));
        Assertions.assertEquals(0, Maths.downDiv(-1, 2));
        Assertions.assertEquals(0, Maths.downDiv(1, -2));

        Assertions.assertEquals(0, Maths.downDiv(0, 5));
        Assertions.assertEquals(0, Maths.downDiv(0, -5));
        Assertions.assertEquals(-5, Maths.downDiv(5, -1));
        Assertions.assertEquals(-5, Maths.downDiv(-5, 1));

        Assertions.assertEquals(0, Maths.downDiv(1, 3));
        Assertions.assertEquals(0, Maths.downDiv(-1, 3));
        Assertions.assertEquals(0, Maths.downDiv(2, 3));
        Assertions.assertEquals(0, Maths.downDiv(-2, 3));
        Assertions.assertEquals(1, Maths.downDiv(3, 3));
        Assertions.assertEquals(-1, Maths.downDiv(-3, 3));
        Assertions.assertEquals(1, Maths.downDiv(4, 3));
        Assertions.assertEquals(-1, Maths.downDiv(-4, 3));
    }

    @Test
    public void testAdd() {
        Assertions.assertThrows(ArithmeticException.class, () -> Maths.abs(Integer.MIN_VALUE));
        Assertions.assertThrows(ArithmeticException.class, () -> Maths.abs(Long.MIN_VALUE));

        Assertions.assertThrows(ArithmeticException.class, () -> Maths.add(Integer.MAX_VALUE, 1));
        Assertions.assertThrows(ArithmeticException.class, () -> Maths.add(Integer.MIN_VALUE, -1));

        Assertions.assertThrows(ArithmeticException.class, () -> Maths.add(Long.MAX_VALUE, 1));
        Assertions.assertThrows(ArithmeticException.class, () -> Maths.add(Long.MIN_VALUE, -1));

        Assertions.assertThrows(ArithmeticException.class, () -> Maths.subtract(Integer.MAX_VALUE, -1));
        Assertions.assertThrows(ArithmeticException.class, () -> Maths.subtract(Integer.MIN_VALUE, 1));

        Assertions.assertThrows(ArithmeticException.class, () -> Maths.subtract(Long.MAX_VALUE, -1));
        Assertions.assertThrows(ArithmeticException.class, () -> Maths.subtract(Long.MIN_VALUE, 1));

        Assertions.assertThrows(ArithmeticException.class, () -> Maths.gcd(-10, 5));
        Assertions.assertThrows(ArithmeticException.class, () -> Maths.gcd(10, -5));

        Assertions.assertEquals(Integer.MAX_VALUE, Maths.abs(Integer.MIN_VALUE + 1));
        Assertions.assertEquals(Long.MAX_VALUE, Maths.abs(Long.MIN_VALUE + 1));

        Assertions.assertEquals(Integer.MAX_VALUE, Maths.add(Integer.MAX_VALUE - 1, 1));
        Assertions.assertEquals(Integer.MIN_VALUE, Maths.add(Integer.MIN_VALUE + 1, -1));
        Assertions.assertEquals(Long.MAX_VALUE, Maths.add(Long.MAX_VALUE - 1, 1));
        Assertions.assertEquals(Long.MIN_VALUE, Maths.add(Long.MIN_VALUE + 1, -1));

        Assertions.assertEquals(Integer.MAX_VALUE, Maths.subtract(Integer.MAX_VALUE - 1, -1));
        Assertions.assertEquals(Integer.MIN_VALUE, Maths.subtract(Integer.MIN_VALUE + 1, 1));
        Assertions.assertEquals(Long.MAX_VALUE, Maths.subtract(Long.MAX_VALUE - 1, -1));
        Assertions.assertEquals(Long.MIN_VALUE, Maths.subtract(Long.MIN_VALUE + 1, 1));
    }

    @Test
    public void testParse() {
        Assertions.assertThrows(NumberFormatException.class, () -> Long.parseLong(Float.toString(1.00f)));
        Assertions.assertThrows(NumberFormatException.class, () -> Integer.parseInt(Long.toString(Long.MAX_VALUE)));
        Assertions.assertEquals("9223372036854775807", BigInteger.valueOf(Long.MAX_VALUE).toString());

        //Assertions.assertEquals(0.001f, Float.parseFloat(Double.toString(0.001D)));
        //Assertions.assertEquals(Float.POSITIVE_INFINITY, Float.parseFloat(Double.toString(Double.MAX_VALUE)));
        //Assertions.assertEquals(0.0, Float.parseFloat(Double.toString(Double.MIN_VALUE)));
        //Assertions.assertEquals(3.4028235E38, Double.parseDouble(Float.toString(Float.MAX_VALUE)));
        //Assertions.assertEquals(1.4E-45, Double.parseDouble(Float.toString(Float.MIN_VALUE)));

        Assertions.assertNull(Numbers.toWrapChar(null));
        Assertions.assertEquals('0', Numbers.toWrapChar('0'));
        Assertions.assertEquals('a', Numbers.toWrapChar('a'));
        Assertions.assertEquals('a', Numbers.toWrapChar("a"));

        Assertions.assertTrue(Numbers.toWrapBoolean(true));
        Assertions.assertFalse(Numbers.toWrapBoolean(false));
        Assertions.assertNull(Numbers.toWrapBoolean(null));
        Assertions.assertFalse(Numbers.toWrapBoolean("null"));

        Assertions.assertNull(Numbers.toWrapInt("null"));
        Assertions.assertNull(Numbers.toWrapInt("123.12"));
        Assertions.assertEquals(123, Numbers.toWrapInt("123"));

        Assertions.assertNull(Numbers.toWrapLong("null"));
        Assertions.assertNull(Numbers.toWrapLong("123.12"));
        Assertions.assertEquals(123, Numbers.toWrapLong("123"));

        Assertions.assertNull(Numbers.toWrapFloat("null"));
        Assertions.assertNull(Numbers.toWrapDouble("null"));
        //Assertions.assertEquals(123.12f, Numbers.toWrapFloat("123.12"));
        //Assertions.assertEquals(123.12d, Numbers.toWrapDouble("123.12"));
    }

    private static long sum(long... array) {
        return LongStream.of(array).sum();
    }

    private static long upDiv2(long dividend, long divisor) {
        if (dividend == 0) {
            return 0;
        }
        if ((dividend % divisor) == 0) {
            return dividend / divisor;
        }
        return new BigDecimal(dividend)
            .divide(new BigDecimal(divisor), MathContext.DECIMAL128)
            .setScale(0, RoundingMode.UP)
            .longValue();
    }

}

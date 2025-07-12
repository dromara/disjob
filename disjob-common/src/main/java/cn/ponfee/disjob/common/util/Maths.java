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

import org.springframework.util.Assert;

/**
 * 数学算术
 * 取模：Modulo Operation
 *
 * @author Ponfee
 */
public class Maths {

    /**
     * 以2为底n的对数
     *
     * @param n the value
     * @return a value of log(n)/log(2)
     */
    public static double log2(double n) {
        return log(n, 2);
    }

    /**
     * 求以base为底n的对数
     * {@link Math#log10(double) }  求以10为底n的对数（lg）
     * {@link Math#log(double)   }  以e为底n的对数（自然对数，ln）
     * {@link Math#log1p(double) }  以e为底n+1的对数
     *
     * @param n    a value
     * @param base 底数
     * @return a double of logarithm
     */
    public static double log(double n, double base) {
        return Math.log(n) / Math.log(base);
    }

    /**
     * rotate shift left，循环左移位操作：0<=n<=32
     *
     * @param x the value
     * @param n shift bit len
     * @return a number of rotate left result
     */
    public static int rotateLeft(int x, int n) {
        return (x << n) | (x >>> (32 - n));
    }

    /**
     * <pre>
     * Returns a long value of bit count mask
     * calculate the bit counts mask long value
     *   a: (1 << bits) - 1
     *   b: -1L ^ (-1L << bits)
     *   c: ~(-1L << bits)
     *   d: Long.MAX_VALUE >>> (63 - bits)
     *
     *  bitsMask(0)  = 0                  : 0000000000000000000000000000000000000000000000000000000000000000
     *  bitsMask(1)  = 1                  : 0000000000000000000000000000000000000000000000000000000000000001
     *  bitsMask(2)  = 3                  : 0000000000000000000000000000000000000000000000000000000000000011
     *  bitsMask(10) = 1023               : 0000000000000000000000000000000000000000000000000000001111111111
     *  bitsMask(20) = 1048575            : 0000000000000000000000000000000000000000000011111111111111111111
     *  bitsMask(63) = 9223372036854775807: 0111111111111111111111111111111111111111111111111111111111111111
     *  bitsMask(64) = -1                 : 1111111111111111111111111111111111111111111111111111111111111111
     * </pre>
     *
     * @param bits the bit count
     * @return a long value
     */
    public static long bitsMask(int bits) {
        Assert.isTrue(bits >= 0 && bits <= Long.SIZE, "bits must range [0,64].");
        return bits == Long.SIZE ? -1 : ~(-1L << bits);
    }

    /**
     * <pe>
     * Reverses int number bits
     * 1711380007              = 01100110000000011001011000100111
     * reverseBits(1711380007) = 11100100011010011000000001100110
     * </pe>
     *
     * @param n the number
     * @return reversed number
     */
    public static int reverseBits(int n) {
        int r = 0;
        for (int i = 0; i < Integer.SIZE; i++) {
            r <<= 1;
            r |= (n & 1);
            n >>= 1;
        }
        return r;
    }

    /**
     * Returns a long value for {@code base}<sup>{@code exponent}</sup>.
     *
     * @param base     the base
     * @param exponent the exponent
     * @return a long value for {@code base}<sup>{@code exponent}</sup>.
     */
    public static long pow(long base, int exponent) {
        if (exponent == 0) {
            return 1;
        }

        long result = base;
        while (--exponent > 0) {
            result *= base;
        }
        return result;
    }

    public static int abs(int n) {
        // Integer.MIN_VALUE & 0x7FFFFFFF = 0
        if (n == Integer.MIN_VALUE) {
            throw new ArithmeticException("Numeric overflow in min int abs value.");
        }
        return (n < 0) ? -n : n;
    }

    public static long abs(long n) {
        if (n == Long.MIN_VALUE) {
            throw new ArithmeticException("Numeric overflow in min long abs value.");
        }
        return (n < 0) ? -n : n;
    }

    /**
     * <pre>
     * upDiv( 7,  3)  =   3
     * upDiv(-7,  3)  =  -3
     * upDiv( 7, -3)  =  -3
     * upDiv(-7, -3)  =   3
     * upDiv(10,  5)  =   2
     * upDiv( 1,  1)  =   1
     * upDiv(-1,  2)  =  -1
     * upDiv( 1, -2)  =  -1
     * upDiv( 0,  5)  =   0
     * upDiv( 0, -5)  =   0
     * upDiv( 5, -1)  =  -5
     * upDiv(-5,  1)  =  -5
     * upDiv( 1,  3)  =   1
     * upDiv(-1,  3)  =  -1
     * upDiv( 2,  3)  =   1
     * upDiv(-2,  3)  =  -1
     *
     * upDiv(x, y)        : 向远于零方向取整
     * downDiv(x, y)      : 向近于零方向取整
     * ceilDiv(x, y)      : 向正无穷方向取整
     * Math.floorDiv(x, y): 向负无穷方向取整
     * </pre>
     *
     * @param x the numerator
     * @param y the denominator
     * @return the quotient of up zero
     */
    public static long upDiv(long x, long y) {
        long q = x / y;
        long r = x % y;

        if (r == 0) {
            return q;
        }
        return q + ((x ^ y) >= 0 ? 1 : -1);
    }

    public static long downDiv(long x, long y) {
        return x / y;
    }

    public static long ceilDiv(long x, long y) {
        long q = x / y;
        long r = x % y;

        if (r == 0) {
            return q;
        }
        return (x ^ y) >= 0 ? q + 1 : q;
    }

    // ------------------------------------------------------------------------add & subtract

    public static int add(int a, int b) {
        if (a > 0 && b > 0 && Integer.MAX_VALUE - b < a) {
            throw new ArithmeticException("Addition overflow int positive number: " + a + " + " + b);
        }
        if (a < 0 && b < 0 && Integer.MIN_VALUE - b > a) {
            throw new ArithmeticException("Addition overflow int negative number: " + a + " + " + b);
        }
        return a + b;
    }

    public static int subtract(int a, int b) {
        if (a > 0 && b < 0 && Integer.MAX_VALUE + b < a) {
            throw new ArithmeticException("Subtraction overflow int positive number: " + a + " - " + b);
        }
        if (a < 0 && b > 0 && Integer.MIN_VALUE + b > a) {
            throw new ArithmeticException("Subtraction overflow int negative number: " + a + " - " + b);
        }
        return a - b;
    }

    public static long add(long a, long b) {
        if (a > 0 && b > 0 && Long.MAX_VALUE - b < a) {
            throw new ArithmeticException("Addition overflow long positive number: " + a + " + " + b);
        }
        if (a < 0 && b < 0 && Long.MIN_VALUE - b > a) {
            throw new ArithmeticException("Addition overflow long negative number: " + a + " + " + b);
        }
        return a + b;
    }

    public static long subtract(long a, long b) {
        if (a > 0 && b < 0 && Long.MAX_VALUE + b < a) {
            throw new ArithmeticException("Subtraction overflow long positive number: " + a + " - " + b);
        }
        if (a < 0 && b > 0 && Long.MIN_VALUE + b > a) {
            throw new ArithmeticException("Subtraction overflow long negative number: " + a + " - " + b);
        }
        return a - b;
    }

    // ------------------------------------------------------------------------gcd

    /**
     * Returns the greatest common divisor
     *
     * @param a the first number
     * @param b the second number
     * @return gcd
     */
    public static int gcd(int a, int b) {
        if (a < 0 || b < 0) {
            throw new ArithmeticException("Calculate gcd unsupported positive number.");
        }
        if (a == 0 || b == 0) {
            return Math.abs(a - b);
        }
        for (int c; (c = a % b) != 0; ) {
            a = b;
            b = c;
        }
        return b;
    }

    /**
     * Returns the greatest common divisor in array
     *
     * @param array the int array
     * @return gcd
     */
    public static int gcd(int[] array) {
        int result = array[0];
        for (int i = 1; i < array.length; i++) {
            result = gcd(result, array[i]);
        }
        return result;
    }

}

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

import com.google.common.math.LongMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Counts prime
 *
 * @author Ponfee
 */
public class Prime {

    private static final Logger LOG = LoggerFactory.getLogger(Prime.class);
    private static final boolean[] PRIME_0_9 = {false, false, true, true, false, true, false, true, false, false};

    public static class Power {
        public static long countPrimes(long m, long n) {
            check(m, n);
            int count = 0;
            for (; m <= n; ++m) {
                if (isPrime(m)) {
                    count += 1;
                }
            }
            return count;
        }

        private static boolean isPrime(long x) {
            if (x < PRIME_0_9.length) {
                return PRIME_0_9[(int) x];
            }

            // 6n,    6n+1, 6n+2,    6n+3,    6n+4,    6n+5
            // 3(2n), 6n+1, 2(3n+1), 3(2n+1), 2(3n+2), 6n+5
            long a = x % 6;
            if (a != 1L && a != 5L) {
                return false;
            }
            for (long i = 2; i * i <= x; ++i) {
                if (x % i == 0) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class Sqrt {
        public static int countPrimes(long m, long n) {
            check(m, n);
            int count = 0;
            for (; m <= n; ++m) {
                if (isPrime(m)) {
                    count += 1;
                }
            }
            return count;
        }

        public static boolean isPrime(long x) {
            if (x < PRIME_0_9.length) {
                return PRIME_0_9[(int) x];
            }

            // 6n,    6n+1, 6n+2,    6n+3,    6n+4,    6n+5
            // 3(2n), 6n+1, 2(3n+1), 3(2n+1), 2(3n+2), 6n+5
            long a = x % 6;
            if (a != 1L && a != 5L) {
                return false;
            }

            long sqrt = LongMath.sqrt(x, RoundingMode.CEILING);
            //long sqrt = (long) Math.ceil(Math.sqrt(x));
            //long sqrt = sqrtNewton(x);
            //long sqrt = sqrtBinary(x);
            for (long i = 4; i <= sqrt; i++) {
                if (x % i == 0) {
                    return false;
                }
            }
            return true;
        }

        public static long sqrtNewton(double value) {
            double r = value / 2, t;
            do {
                t = r;
                r = (t + value / t) / 2;
            } while (t - r > 1.0D);

            return (long) Math.ceil(r);
        }

        public static long sqrtBinary(long value) {
            long lower = 1, upper = value, root, square;
            do {
                root = lower + (upper - lower) / 2;
                square = root * root;
                if (square == value) {
                    //System.out.println(value + ": [" + root + ", " + lower + ", " + upper + "]");
                    return root;
                } else if (square > value) {
                    upper = root;
                } else {
                    lower = root;
                }
            } while (upper - lower > 1);
            //System.out.println(value + ": (" + root + ", " + lower + ", " + upper + ")");

            return upper;
        }
    }

    /**
     * 埃拉托色尼筛法(埃氏筛法)
     */
    public static class EratosthenesSieve {
        public static int countPrimes(int n) {
            check(n);
            boolean[] isPrime = new boolean[n + 1];
            Arrays.fill(isPrime, true);
            int count = 0;
            for (int i = 2; i <= n; ++i) {
                if (isPrime[i]) {
                    count += 1;
                    if ((long) i * i <= n) {
                        for (int j = i * i; j <= n; j += i) {
                            isPrime[j] = false;
                        }
                    }
                }
            }
            return count;
        }
    }

    /**
     * 欧拉筛法（线性筛，埃氏筛法的优化版）
     */
    public static class EulerSieve {
        public static int countPrimes(int n) {
            check(n);
            List<Integer> primes = new ArrayList<>(5761455);
            boolean[] isPrime = new boolean[n + 1];
            Arrays.fill(isPrime, true);
            for (int i = 2; i <= n; ++i) {
                if (isPrime[i]) {
                    primes.add(i);
                }
                for (int j = 0, t; j < primes.size() && (t = i * primes.get(j)) <= n; ++j) {
                    isPrime[t] = false;
                    if (i % primes.get(j) == 0) {
                        break;
                    }
                }
            }
            return primes.size();
        }
    }

    /**
     * Miller Rabin素性检验
     *
     * @see java.math.BigInteger#isProbablePrime(int)
     */
    public static class MillerRabin {
        public static long countPrimes(long m, long n) {
            check(m, n);
            long a = m, size = n - m + 1;
            long count = 0;
            for (; m <= n; ++m) {
                if (LongMath.isPrime(m)) {
                    count += 1;
                }
            }
            LOG.info("Count primes: [{}, {}]({})={}", a, n, size, count);
            return count;
        }
    }

    private static void check(long m, long n) {
        Assert.isTrue(0 <= m && m <= n, "Invalid [" + m + ", " + n + "]");
    }

    private static void check(long n) {
        Assert.isTrue(n >= 0, "N must greater than 0.");
    }

}

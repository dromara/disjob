/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.test.util;

import com.google.common.math.LongMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static class Power {
        public static long countPrimes(long m, long n) {
            assert m > 0;
            assert m <= n;
            int count = 0;
            if (m < 4) {
                m = 4;
                count += (m == 2 ? 1 : 2);
            }
            for (; m <= n; ++m) {
                if (isPrime(m)) {
                    count += 1;
                }
            }
            return count;
        }

        private static boolean isPrime(long x) {
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
            assert m > 0;
            assert m <= n;
            int count = 0;
            if (m < 4) {
                m = 4;
                count += (m == 2 ? 1 : 2);
            }
            for (; m <= n; ++m) {
                if (isPrime(m)) {
                    count += 1;
                }
            }
            return count;
        }

        public static boolean isPrime(long x) {
            long a = x % 6;
            if (a != 1L && a != 5L) {
                return false;
            }

            //for (long i = 4, sqrt = LongMath.sqrt(x, RoundingMode.CEILING); i <= sqrt; i++) {
            for (long i = 4, sqrt = (long) Math.sqrt(x) + 1; i <= sqrt; i++) {
            //for (long i = 4, sqrt = sqrtNewton(x); i <= sqrt; i++) {
            //for (long i = 4, sqrt = sqrtBinary(x); i <= sqrt; i++) {
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
            assert n > 0;
            boolean[] isPrime = new boolean[n];
            Arrays.fill(isPrime, true);
            int ans = 0;
            for (int i = 2; i < n; ++i) {
                if (isPrime[i]) {
                    ans += 1;
                    if ((long) i * i < n) {
                        for (int j = i * i; j < n; j += i) {
                            isPrime[j] = false;
                        }
                    }
                }
            }
            return ans;
        }
    }

    /**
     * 欧拉筛法（线性筛，埃氏筛法的优化版）
     */
    public static class EulerSieve {
        public static int countPrimes(int n) {
            assert n > 0;
            List<Integer> primes = new ArrayList<>(5761455);
            boolean[] isPrime = new boolean[n];
            Arrays.fill(isPrime, true);
            for (int i = 2; i < n; ++i) {
                if (isPrime[i]) {
                    primes.add(i);
                }
                for (int j = 0, t; j < primes.size() && (t = i * primes.get(j)) < n; ++j) {
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
            long  a = m, size = n - m + 1;
            assert m > 0;
            assert m <= n;
            long ans = 0;
            for (; m <= n; ++m) {
                if (isPrime(m)) {
                    ans += 1;
                }
            }
            LOG.info("Count primes: [{}, {}]({})={}", a, n, size, ans);
            return ans;
        }

        private static boolean isPrime(long x) {
            return LongMath.isPrime(x);
        }
    }

}

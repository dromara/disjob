/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.test.common.util;

import cn.ponfee.scheduler.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.scheduler.common.util.Bytes;
import cn.ponfee.scheduler.common.util.ClassUtils;
import cn.ponfee.scheduler.common.util.Networks;
import cn.ponfee.scheduler.core.enums.RunState;
import com.google.common.math.IntMath;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Hash test
 *
 * @author Ponfee
 */
public class HashTest {

    private Integer get(TreeMap<String, Integer> treeMap, String key) {
        Integer val = treeMap.get(key);
        if (val != null) {
            return val;
        }
        SortedMap<String, Integer> headMap = treeMap.headMap(key);
        return headMap.isEmpty() ? treeMap.firstEntry().getValue() : treeMap.get(headMap.lastKey());
    }

    @Test
    public void testConsistentHash() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Set<String> strings = map.keySet();
        Assertions.assertEquals(strings.size(), 2);
        map.remove("a");
        Assertions.assertEquals(strings.size(), 1);

        Assertions.assertEquals(IntMath.pow(1, 2), 1);
        Assertions.assertEquals(IntMath.pow(2, 2), 4);
        Assertions.assertEquals(IntMath.pow(3, 2), 9);
        Assertions.assertEquals(IntMath.pow(9, 2), 81);
        final long currentTimeMillis = System.currentTimeMillis();
        System.out.println(currentTimeMillis);
        System.out.println(TimeUnit.SECONDS.convert(currentTimeMillis, TimeUnit.MILLISECONDS));
        TreeMap<String, Integer> limit = IntStream.range(1, 10)
            .mapToObj(Integer::valueOf)
            .collect(Collectors.toMap(e -> String.format("%02d", e * 3), e -> e * 10 + 1, (v1, v2) -> v1, TreeMap::new));

        System.out.println(limit);
        Assertions.assertEquals(11, (int) get(limit, "02"));
        Assertions.assertEquals(11, (int) get(limit, "03"));
        Assertions.assertEquals(11, (int) get(limit, "04"));
        Assertions.assertEquals(11, (int) get(limit, "05"));
        Assertions.assertEquals(21, (int) get(limit, "06"));
        Assertions.assertEquals(91, (int) get(limit, "27"));
        Assertions.assertEquals(91, (int) get(limit, "28"));
    }

    @Test
    public void testMD5() throws ClassNotFoundException {
        System.out.println(RunState.class.getEnumConstants()[0]);
        System.out.println(RunState.values()[0]);

        for (int i = 0; i < 100; i++) {
            if ((i & 0x07) == 0) {
                System.out.print(i + ", ");
            }
        }

        System.out.println(Long.MAX_VALUE);
        Assertions.assertEquals(1000000L, 1000000);
        Assertions.assertEquals(16, 0x10);
        Assertions.assertTrue(ClassUtils.QUALIFIED_CLASS_NAME_PATTERN.matcher("org.springframework.context.annotation.ConfigurationClassParser$ImportStack").matches());
        Assertions.assertEquals("900150983cd24fb0d6963f7d28e17f72", DigestUtils.md5Hex("abc".getBytes(StandardCharsets.UTF_8)));
        Assertions.assertEquals(" abc \n\r ".trim(), "abc");
        Assertions.assertEquals(String.class, Class.forName("java.lang.String"));
    }

    static long round = 1_000_000_000L;

    @Test
    public void testNexFloat() {
        for (long i = 0; i < round; i++) {
            ThreadLocalRandom.current().nextFloat();
        }
    }

    @Test
    public void testNextInt() {
        for (long i = 0; i < round; i++) {
            ThreadLocalRandom.current().nextInt(100);
        }
    }

    @Test
    public void testUuid() {
        for (int i = 0; i < 10000; i++) {
            UUID uuid = UUID.randomUUID();
            String uid = uuid.toString().replace("-", "");
            Assertions.assertEquals(uid, uuid32(uuid));
            Assertions.assertEquals(uid, Hex.encodeHexString(uuid(uuid)));
        }
    }

    @Test
    public void testExpression() {
        String basePackage1 = ClassUtils.getPackagePath(getClass());
        Assertions.assertTrue((basePackage1.endsWith("/")));

        String basePackage2 = basePackage1.substring(0, basePackage1.length() - 1);
        Assertions.assertFalse((basePackage2.endsWith("/")));
        Assertions.assertEquals(basePackage2 + "/", basePackage1);

        System.out.println(basePackage1);
        System.out.println(basePackage2);

        int i = 9;
        int num = i < 10 ? i * 2 : round(i / 0);
        Assertions.assertEquals(num, 18);
    }

    @Test
    public void testOperator() {
        String s = "abc:123";
        String[] arr1 = s.split(":");
        String[] arr2 = s.split(":");
        Assertions.assertEquals("abc", arr1[0]);
        Assertions.assertFalse(arr1[0] == arr2[0]);
        Assertions.assertTrue(Object.class.isAssignableFrom(String.class));
        Assertions.assertEquals(1, 1 ^ 0);
        Assertions.assertEquals(0, 1 ^ 1);
    }

    private int round(int rounding) {
        throw new RuntimeException();
    }

    private static String uuid32(UUID uuid) {
        return Bytes.toHex(uuid.getMostSignificantBits(), true)
            + Bytes.toHex(uuid.getLeastSignificantBits(), true);
    }

    private static byte[] uuid(UUID uuid) {
        long most = uuid.getMostSignificantBits(),
            least = uuid.getLeastSignificantBits();
        return new byte[]{
            (byte) (most >>> 56), (byte) (most >>> 48),
            (byte) (most >>> 40), (byte) (most >>> 32),
            (byte) (most >>> 24), (byte) (most >>> 16),
            (byte) (most >>> 8), (byte) (most),

            (byte) (least >>> 56), (byte) (least >>> 48),
            (byte) (least >>> 40), (byte) (least >>> 32),
            (byte) (least >>> 24), (byte) (least >>> 16),
            (byte) (least >>> 8), (byte) (least)
        };
    }

    @Test
    public void testNet() {
        System.out.println(Networks.getHostIp());
    }

    @Test
    public void testThreadPool() throws InterruptedException {
        ThreadPoolExecutor pool = ThreadPoolExecutors.create(
            1,
            10,
            300,
            100,
            "test",
            ThreadPoolExecutors.DISCARD
        );
        ThreadPoolExecutors.shutdown(pool, 1);
        pool.shutdownNow();
        pool.submit(() -> {
            System.out.println("------------a");
            System.out.println(1 / 0);
            System.out.println("------------b");
        });
        System.out.println("------------1");
        Thread.sleep(4000);
    }

}

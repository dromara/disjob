/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.test.job.util;

import cn.ponfee.scheduler.common.base.tuple.Tuple2;
import cn.ponfee.scheduler.common.util.Collects;
import cn.ponfee.scheduler.common.util.Numbers;
import cn.ponfee.scheduler.common.util.URLCodes;
import cn.ponfee.scheduler.core.enums.JobType;
import cn.ponfee.scheduler.core.enums.Operations;
import cn.ponfee.scheduler.core.param.ExecuteTaskParam;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * @author Ponfee
 */
public class CommonTest {

    @Test
    public void testPath() {
        List<String> list = Collections.emptyList();
        Assertions.assertEquals("classpath*:xml/*.xml", path(list, 0));

        list = Arrays.asList("a");
        Assertions.assertEquals("classpath*:a/xml/*.xml", path(list, -1));
        Assertions.assertEquals("classpath*:a/**/xml/*.xml", path(list, 0));
        Assertions.assertEquals("classpath*:/**/a/xml/*.xml", path(list, 1));

        list = Arrays.asList("a","b","c");
        Assertions.assertEquals("classpath*:a/b/c/xml/*.xml", path(list, -1));
        Assertions.assertEquals("classpath*:a/b/c/**/xml/*.xml", path(list, 0));
        Assertions.assertEquals("classpath*:a/b/**/c/xml/*.xml", path(list, 1));
        Assertions.assertEquals("classpath*:a/**/b/c/xml/*.xml", path(list, 2));
        Assertions.assertEquals("classpath*:/**/a/b/c/xml/*.xml", path(list, 3));
        Assertions.assertEquals("classpath*:/**/a/b/c/xml/*.xml", path(list, 4));
    }

    private static String path(List<String> list, int wildcardLastIndex) {
        String path;
        if (list.size() == 0) {
            path = "";
        } else if (wildcardLastIndex == 0) {
            path = String.join("/", list) + "/**/";
        } else if (wildcardLastIndex < 0) {
            path = String.join("/", list) + "/";
        } else if (list.size() <= wildcardLastIndex) {
            path = "/**/" + String.join("/", list) + "/";
        } else {
            int pos = list.size() - wildcardLastIndex;
            path = String.join("/", list.subList(0, pos)) + "/**/" + String.join("/", list.subList(pos, list.size())) + "/";
        }
        return MessageFormat.format("classpath*:{0}xml/*.xml", path);
    }

    @Test
    public void testURLString() {
        Assertions.assertEquals("abc%2C123", URLCodes.encodeURIComponent("abc,123"));
        Assertions.assertEquals("abc,123", URLCodes.decodeURIComponent("abc%2C123"));
        Assertions.assertEquals("127.0.0.1%3A8080", URLCodes.encodeURIComponent("127.0.0.1:8080"));
        Assertions.assertEquals("127.0.0.1:8080", URLCodes.decodeURIComponent("127.0.0.1%3A8080"));
        Assertions.assertEquals("3988904755200", URLCodes.encodeURIComponent("3988904755200"));
    }

    @Test
    public void testTaskParam() {
        ExecuteTaskParam param1 = new ExecuteTaskParam(
            Operations.TRIGGER,
            ThreadLocalRandom.current().nextLong(),
            ThreadLocalRandom.current().nextLong(),
            ThreadLocalRandom.current().nextLong(),
            JobType.NORMAL,
            ThreadLocalRandom.current().nextLong(),
            null
        );
        System.out.println(param1);
        ExecuteTaskParam param2 = ExecuteTaskParam.deserialize(param1.serialize());
        Assertions.assertFalse(param1 == param2);
        Assertions.assertEquals(param1, param2);
    }

    @Test
    public void testTime() {
        System.out.println((System.currentTimeMillis() / 1000) * 1000 + 999);
    }

    @Test
    public void testArrayPartition() {
        int[] ints = IntStream.range(0, 5).toArray();
        System.out.println(Arrays.toString(ints));
        Collects.partition(ints, 2)
            .stream()
            .peek(e -> System.out.println(Arrays.toString(e)))
            .skip(1)
            .forEach(e -> Assertions.assertTrue(Arrays.equals(new int[]{3, 4}, e)));

        System.out.println("-----\n");
        ints = IntStream.range(0, 6).toArray();
        System.out.println(Arrays.toString(ints));
        Collects.partition(ints, 2)
            .stream()
            .peek(e -> System.out.println(Arrays.toString(e)))
            .skip(1)
            .forEach(e -> Assertions.assertTrue(Arrays.equals(new int[]{3, 4, 5}, e)));

        System.out.println("-----\n");
        ints = IntStream.range(0, 3).toArray();
        System.out.println(Arrays.toString(ints));
        Collects.partition(ints, 2)
            .stream()
            .peek(e -> System.out.println(Arrays.toString(e)))
            .skip(1)
            .forEach(e -> Assertions.assertTrue(Arrays.equals(new int[]{2}, e)));


        System.out.println("-----\n");
        ints = IntStream.range(0, 1).toArray();
        System.out.println(Arrays.toString(ints));
        Collects.partition(ints, 5)
            .stream()
            .peek(e -> System.out.println(Arrays.toString(e)))
            .skip(1)
            .forEach(e -> Assertions.assertTrue(Arrays.equals(new int[]{0}, e)));

        System.out.println("-----\n");
        ints = IntStream.range(0, 256).toArray();
        System.out.println("origin: " + Arrays.toString(ints));
        Collects.partition(ints, 5)
            .stream()
            .peek(e -> System.out.println("partitioned: " + Arrays.toString(e)))
            .skip(4)
            .peek(e -> System.out.println("last: " + Arrays.toString(e)))
            .forEach(e -> Assertions.assertTrue(Arrays.equals(new int[]{205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255}, e)));
    }

    @Test
    public void testNumberSplit() {
        System.out.println(Arrays.toString(Numbers.slice(3, 4)));
        System.out.println("------\n");
        System.out.println(Numbers.partition(31, 4));
        System.out.println(Numbers.partition(0, 1));
        System.out.println(Numbers.partition(0, 4));
        System.out.println(Numbers.partition(3, 4));
        System.out.println(Numbers.partition(6, 4));
        System.out.println(Numbers.partition(5, 2));
        System.out.println(Numbers.partition(47, 1));
        System.out.println(Numbers.partition(47, 2));
        System.out.println(Numbers.partition(256, 2));
        System.out.println(Numbers.partition(256, 4));
        for (int i = 0; i < 1000000; i++) {
            int number = ThreadLocalRandom.current().nextInt(100000) + 1, size = ThreadLocalRandom.current().nextInt(31) + 1;
            List<Tuple2<Integer, Integer>> split = Numbers.partition(number, size);
            Assertions.assertTrue(Collects.getFirst(split).a == 0);
            Assertions.assertTrue(Collects.getFirst(split).b == (number + size - 1) / size - 1);
            Assertions.assertTrue(Collects.getLast(split).b == number - 1);
            Assertions.assertTrue(Collects.getLast(Numbers.partition(number, size)).b == number - 1);
        }
    }

}

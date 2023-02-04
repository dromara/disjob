/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.test.common.util;

import cn.ponfee.scheduler.common.util.Jsons;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * @author Ponfee
 */
public class TreeMapTest {

    @Test
    @Disabled
    public void testTreeMap() {
        Map<String, String> executing = new TreeMap<>(); // TreeMap HashMap
        IntStream.range(0, 256)
            .mapToObj(i -> String.format("%03d", i))
            .forEach(e -> executing.put(e, "v-" + e));
        Map<String, String> finished = new TreeMap<>();

        System.out.println("-----------------before");
        int beforeSize = executing.size();
        String beforeFull = Jsons.toJson(executing);
        String beforeEmpty = Jsons.toJson(finished);
        System.out.println(beforeFull);
        System.out.println(beforeEmpty);
        for (int i = 0; !executing.isEmpty(); i++) {
            for (Iterator<Map.Entry<String, String>> iter = executing.entrySet().iterator(); iter.hasNext(); ) {
                Map.Entry<String, String> shard = iter.next();
                String value = shard.getValue();
                if (ThreadLocalRandom.current().nextInt(9) < 3) {
                    String s1 = shard.getKey();
                    iter.remove();
                    finished.put(shard.getKey(), value);
                    //finished.put(s1, value);
                    String s2 = shard.getKey();
                    System.out.println("remove_before_and_after: " + s1 + " -> " + s2);
                }
            }
        }

        System.out.println("\n\n-----------------after");
        String afterFull = Jsons.toJson(finished);
        String afterEmpty = Jsons.toJson(executing);
        System.out.println(afterFull);
        System.out.println(afterEmpty);

        int afterSize = finished.size();
        Assertions.assertEquals(beforeEmpty, afterEmpty);
        Assertions.assertEquals(beforeSize, afterSize);
        Assertions.assertEquals(beforeFull, afterFull);
    }

    @Test
    public void testConcurrentHashSet() {
        Set<String> set = ConcurrentHashMap.newKeySet();
        Assertions.assertTrue(set.isEmpty());

        set.add("a");
        set.add("b");
        Assertions.assertEquals(set.size(), 2);

        set.add("b");
        set.add("c");
        Assertions.assertEquals(set.size(), 3);
    }


    @org.junit.jupiter.api.Test
    public void testMap() {
        Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        map1.put("c", 3);
        map1.put("d", 3);
        Assertions.assertEquals("{a=1, b=2, c=3, d=3}", map1.toString());

        map1.put("c", 5);
        Assertions.assertEquals("{a=1, b=2, c=5, d=3}", map1.toString());

        Collection<Integer> values1 = map1.values();
        Assertions.assertTrue(!(values1 instanceof List));

        Assertions.assertEquals("[1, 2, 5, 3]", values1.toString());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> values1.add(5));

        values1.remove(5);
        Assertions.assertEquals("[1, 2, 3]", values1.toString());

        map1.put("e", 7);
        Assertions.assertEquals("[1, 2, 3, 7]", values1.toString());

        // --------------------------------------------------map2
        Map<String, Integer> map2 = Collections.unmodifiableMap(map1);
        Assertions.assertEquals("{a=1, b=2, d=3, e=7}", map2.toString());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map2.put("f", 8));

        Collection<Integer> values2 = map2.values();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> values2.add(10));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> values2.remove(1));
    }

    @Test
    public void testComputeIfAbsentNull() {
        Map<String, Integer> map = new ConcurrentHashMap<>();

        map.computeIfAbsent("a", key -> null);
        map.computeIfAbsent("a", key -> null);
        Assertions.assertNull(map.get("a"));
    }

}

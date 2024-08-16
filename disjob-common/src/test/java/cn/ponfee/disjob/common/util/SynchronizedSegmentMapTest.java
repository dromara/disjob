package cn.ponfee.disjob.common.util;

import cn.ponfee.disjob.common.collect.SynchronizedSegmentMap;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SynchronizedSegmentMap Test
 *
 * @author Ponfee
 */
public class SynchronizedSegmentMapTest {

    @Test
    public void test1() {

        String s = null;
        System.out.println("sabc " + s);
        for (int i = 1; i <= 10; i++) {
            System.out.print((1 << i) + ", ");
        }

        System.out.println("\n---------------------\n");

        for (int i = 0; i < 100; i++) {
            System.out.print((i & 31) + ", ");
        }

        System.out.println("\n---------------------\n");

        SynchronizedSegmentMap<Integer, Integer> map = new SynchronizedSegmentMap<>(14);
        for (int i = 0; i < 40000; i++) {
            int x = ThreadLocalRandom.current().nextInt();
            map.execute(x, e -> e.put(x, x));
        }
        map.execute(System.out::println);

        Map<Integer, Integer> m = new HashMap<>();
        m.put(1, 1);
        m.put(2, 2);
        m.put(3, 3);

        System.out.println(m);
        assertThat(m.size()).isEqualTo(3);

        m.keySet().remove(1);
        System.out.println(m);
        assertThat(m.size()).isEqualTo(2);

        m.entrySet().remove(Pair.of(2, 2));
        System.out.println(m);
        assertThat(m.size()).isEqualTo(1);

        m.values().remove(3);
        System.out.println(m);
        assertThat(m.size()).isEqualTo(0);
    }

}

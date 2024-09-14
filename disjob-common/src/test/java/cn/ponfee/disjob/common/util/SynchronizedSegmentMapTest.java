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

import cn.ponfee.disjob.common.collect.SynchronizedSegmentMap;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Disabled;
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

    @Test @Disabled
    public void test1() {
        String s = null;
        System.out.println("abc " + s);
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

    @Test @Disabled
    public void test2() {
        SynchronizedSegmentMap<String, String> map = new SynchronizedSegmentMap<>(14);
        for (int i = 0; i < 30000; i++) {
            String s = RandomStringUtils.randomGraph(1+ThreadLocalRandom.current().nextInt(20));
            map.execute(s, e -> e.put(s, s));
        }
        map.execute(System.out::println);
    }

}

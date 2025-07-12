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
import org.apache.commons.lang3.mutable.MutableLong;
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
class SynchronizedSegmentMapTest {

    @Test
    void testPut() {
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
        for (int i = 0; i < 100; i++) {
            int x = ThreadLocalRandom.current().nextInt();
            map.put(x, x);
        }
        map.forEach((k, v) -> System.out.println(k + " -> " + v));

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

    @Test
    @Disabled
    void testExecute() {
        int total = 320000, expectSegmentSize = total / 16;
        SynchronizedSegmentMap<String, String> map1 = new SynchronizedSegmentMap<>(14);
        for (int i = 0; i < total; i++) {
            String s = UuidUtils.uuid32();
            map1.put(s, s);
        }
        MutableLong size1 = new MutableLong(0);
        map1.execute(e -> size1.add(e.size()));
        System.out.println("Map1 total: " + size1.getValue());
        map1.execute(e -> System.out.println(Numbers.percent(e.size(), expectSegmentSize, 2)));

        System.out.println("--------------");
        SynchronizedSegmentMap<Long, String> map2 = new SynchronizedSegmentMap<>(14);
        for (int i = 0; i < total; i++) {
            long k = ThreadLocalRandom.current().nextLong();
            map2.put(k, k + "");
        }
        MutableLong size2 = new MutableLong(0);
        map1.execute(e -> size2.add(e.size()));
        System.out.println("Map2 total: " + size2.getValue());
        map2.execute(e -> System.out.println(Numbers.percent(e.size(), expectSegmentSize, 2)));
    }

}

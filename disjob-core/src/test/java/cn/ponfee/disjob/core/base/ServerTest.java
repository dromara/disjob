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

package cn.ponfee.disjob.core.base;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Server test
 *
 * @author Ponfee
 */
public class ServerTest {

    @Test
    public void testSameServer() {
        Worker worker0 = new Worker("group-a", "workerId1", "localhost", 80);
        Worker worker1 = new Worker("group-a", "workerId1", "localhost", 80);
        Worker worker2 = new Worker("group-a", "workerId2", "localhost", 80);
        Worker worker3 = new Worker("group-b", "workerId2", "localhost", 80);
        assertThat(worker0.equals(worker1)).isTrue();
        assertThat(worker1.matches(worker2)).isTrue();
        assertThat(worker1.equals(worker2)).isFalse();
        assertThat(worker3.matches(worker2)).isFalse();
    }

    @Test
    public void testSortWorker() {
        List<Worker> list = new ArrayList<>();
        list.add(new Worker("group-a", "workerId1", "localhost1", 80));
        list.add(new Worker("group-a", "workerId2", "localhost1", 80));
        list.add(new Worker("group-b", "workerId1", "localhost1", 80));
        list.add(new Worker("group-b", "workerId1", "localhost2", 80));
        list.add(new Worker("group-b", "workerId1", "localhost2", 82));
        Collections.shuffle(list);
        System.out.println(list);
        list.sort(Comparator.naturalOrder());
        System.out.println(list);
        assertThat(list.toString()).isEqualTo("[group-a:workerId1:localhost1:80, group-b:workerId1:localhost1:80, group-b:workerId1:localhost2:80, group-b:workerId1:localhost2:82, group-a:workerId2:localhost1:80]");
    }

    @Test
    public void testSortSupervisor() {
        List<Supervisor> list = new ArrayList<>();
        list.add(new Supervisor("supervisor1", 80));
        list.add(new Supervisor("supervisor1", 85));
        list.add(new Supervisor("supervisor2", 85));
        list.add(new Supervisor("supervisor3", 10));
        Collections.shuffle(list);

        System.out.println(list);
        System.out.println(Collections.binarySearch(list, new Supervisor("supervisor1", 80)));
        list.sort(Comparator.naturalOrder());
        System.out.println(list);
        assertThat(list.toString()).isEqualTo("[supervisor1:80, supervisor1:85, supervisor2:85, supervisor3:10]");
        assertThat(Collections.binarySearch(list, new Supervisor("supervisor1", 80))).isEqualTo(0);

        assertThat(Collections.binarySearch(Collections.emptyList(), 1)).isEqualTo(-1);
        assertThat(Collections.binarySearch(Collections.singletonList(1), 1)).isEqualTo(0);
        assertThat(Collections.binarySearch(Arrays.asList(1, 1), 1)).isEqualTo(0);
        assertThat(Collections.binarySearch(Arrays.asList(1, 1, 1), 1)).isEqualTo(1);
        assertThat(Collections.binarySearch(Arrays.asList(1, 1, 1, 1), 1)).isEqualTo(1);
        assertThat(Collections.binarySearch(Arrays.asList(1, 1, 1, 1, 1), 1)).isEqualTo(2);
        assertThat(Collections.binarySearch(Arrays.asList(2, 3, 4), 1)).isEqualTo(-1);
        assertThat(Collections.binarySearch(Arrays.asList(2, 3, 4), 5)).isEqualTo(-4);
        assertThat(Collections.binarySearch(Arrays.asList(2, 4, 6), 3)).isEqualTo(-2);
        assertThat(Collections.binarySearch(Arrays.asList(2, 4, 6), 5)).isEqualTo(-3);
    }

}

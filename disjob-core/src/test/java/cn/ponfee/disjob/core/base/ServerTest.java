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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Server test
 *
 * @author Ponfee
 */
public class ServerTest {

    @Test
    public void testSameServer() {
        Worker worker1 = new Worker("group-a", "workerId1", "localhost", 80);
        Worker worker2 = new Worker("group-a", "workerId2", "localhost", 80);
        Worker worker3 = new Worker("group-b", "workerId2", "localhost", 80);
        assertThat(worker1.sameWorker(worker2)).isTrue();
        assertThat(worker1.sameServer(worker2)).isTrue();
        assertThat(worker3.sameWorker(worker2)).isFalse();
    }

}

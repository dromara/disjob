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

package cn.ponfee.disjob.core.enums;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Execute state test
 *
 * @author Ponfee
 */
public class ExecuteStateTest {

    @Test
    public void testOf() {
        assertThat(ExecuteState.of(10)).isSameAs(ExecuteState.WAITING);
    }

    @Test
    public void testEquals() {
        Assertions.assertNotSame(RunState.class.getEnumConstants(), RunState.class.getEnumConstants());
        Assertions.assertNotSame(RunState.values(), RunState.values());
        Assertions.assertNotSame(RunState.class.getEnumConstants(), RunState.values());

        RunState[] enumConstants = RunState.class.getEnumConstants();
        Assertions.assertEquals("[WAITING, RUNNING, PAUSED, COMPLETED, CANCELED]", Arrays.toString(enumConstants));
        enumConstants[0] = RunState.PAUSED;
        Assertions.assertEquals("[PAUSED, RUNNING, PAUSED, COMPLETED, CANCELED]", Arrays.toString(enumConstants));
        Assertions.assertEquals("[WAITING, RUNNING, PAUSED, COMPLETED, CANCELED]", Arrays.toString(RunState.class.getEnumConstants()));

        RunState[] values = RunState.values();
        Assertions.assertEquals("[WAITING, RUNNING, PAUSED, COMPLETED, CANCELED]", Arrays.toString(values));
        values[0] = RunState.PAUSED;
        Assertions.assertEquals("[PAUSED, RUNNING, PAUSED, COMPLETED, CANCELED]", Arrays.toString(values));
        Assertions.assertEquals("[WAITING, RUNNING, PAUSED, COMPLETED, CANCELED]", Arrays.toString(RunState.values()));
    }

}

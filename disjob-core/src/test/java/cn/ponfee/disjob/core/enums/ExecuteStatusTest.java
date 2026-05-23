/*
 * Copyright 2022-2026 Ponfee (http://www.ponfee.cn/)
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
 * Execute status test
 *
 * @author Ponfee
 */
public class ExecuteStatusTest {

    @Test
    public void testOf() {
        assertThat(ExecuteStatus.of(10)).isSameAs(ExecuteStatus.WAITING);
    }

    @Test
    public void testEquals() {
        Assertions.assertNotSame(RunStatus.class.getEnumConstants(), RunStatus.class.getEnumConstants());
        Assertions.assertNotSame(RunStatus.values(), RunStatus.values());
        Assertions.assertNotSame(RunStatus.class.getEnumConstants(), RunStatus.values());

        RunStatus[] enumConstants = RunStatus.class.getEnumConstants();
        Assertions.assertEquals("[WAITING, RUNNING, PAUSED, COMPLETED, CANCELED]", Arrays.toString(enumConstants));
        enumConstants[0] = RunStatus.PAUSED;
        Assertions.assertEquals("[PAUSED, RUNNING, PAUSED, COMPLETED, CANCELED]", Arrays.toString(enumConstants));
        Assertions.assertEquals("[WAITING, RUNNING, PAUSED, COMPLETED, CANCELED]", Arrays.toString(RunStatus.class.getEnumConstants()));

        RunStatus[] values = RunStatus.values();
        Assertions.assertEquals("[WAITING, RUNNING, PAUSED, COMPLETED, CANCELED]", Arrays.toString(values));
        values[0] = RunStatus.PAUSED;
        Assertions.assertEquals("[PAUSED, RUNNING, PAUSED, COMPLETED, CANCELED]", Arrays.toString(values));
        Assertions.assertEquals("[WAITING, RUNNING, PAUSED, COMPLETED, CANCELED]", Arrays.toString(RunStatus.values()));
    }

}

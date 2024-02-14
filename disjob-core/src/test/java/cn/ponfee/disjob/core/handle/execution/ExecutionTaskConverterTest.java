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

package cn.ponfee.disjob.core.handle.execution;

import cn.ponfee.disjob.core.enums.ExecuteState;
import cn.ponfee.disjob.core.model.SchedTask;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Execution task converter test
 *
 * @author Ponfee
 */
public class ExecutionTaskConverterTest {

    @Test
    public void test1() {
        SchedTask task = new SchedTask();
        task.setExecuteState(ExecuteState.EXECUTING.value());

        ExecutedTask executedTask = ExecutionTaskConverter.INSTANCE.toExecutedTask(task);
        Assertions.assertEquals(executedTask.getExecuteState(), ExecuteState.EXECUTING);
    }
}

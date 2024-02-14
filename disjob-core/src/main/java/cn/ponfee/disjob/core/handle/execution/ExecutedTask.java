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
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Executed task
 *
 * @author Ponfee
 */
@Getter
@Setter
public class ExecutedTask extends AbstractExecutionTask {
    private static final long serialVersionUID = -4625053001297718912L;

    /**
     * 执行状态
     */
    private ExecuteState executeState;

    public static List<ExecutedTask> convert(List<SchedTask> tasks) {
        if (tasks == null) {
            return null;
        }
        return tasks.stream()
            .map(ExecutionTaskConverter.INSTANCE::toExecutedTask)
            .collect(Collectors.toList());
    }

}

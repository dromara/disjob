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

package cn.ponfee.disjob.worker.handle;

import cn.ponfee.disjob.core.param.worker.SplitTaskParam;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Split task structure.
 *
 * @author Ponfee
 */
@Getter
@Setter
@NoArgsConstructor
public class SplitTask implements java.io.Serializable {
    private static final long serialVersionUID = 5200874217689134007L;

    private String taskParam;

    public SplitTask(String taskParam) {
        this.taskParam = taskParam;
    }

    @Override
    public String toString() {
        return taskParam;
    }

    public SplitTaskParam convert() {
        return new SplitTaskParam(taskParam);
    }

}
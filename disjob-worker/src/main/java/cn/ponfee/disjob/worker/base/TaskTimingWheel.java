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

package cn.ponfee.disjob.worker.base;

import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;

import java.util.Objects;

/**
 * Timing wheel for execute sched task.
 *
 * @author Ponfee
 */
public class TaskTimingWheel extends TimingWheel<ExecuteTaskParam> {
    private static final long serialVersionUID = 5234431161365689615L;

    public TaskTimingWheel(long tickMs, int ringSize) {
        super(tickMs, ringSize);
    }

    @Override
    protected boolean verify(ExecuteTaskParam param) {
        Objects.requireNonNull(param, "Execute task param cannot be null.");
        Objects.requireNonNull(param.getWorker(), "Execute task worker cannot be null.");
        return true;
    }

}

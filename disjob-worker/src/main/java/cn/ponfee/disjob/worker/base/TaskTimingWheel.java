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
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;

import java.util.Objects;

/**
 * Timing wheel for execute sched task.
 *
 * @author Ponfee
 */
public class TaskTimingWheel extends TimingWheel<ExecuteTaskParam> {
    private static final long serialVersionUID = 5234431161365689615L;

    private final Worker.Current currentWorker;

    public TaskTimingWheel(Worker.Current currentWorker, long tickMs, int ringSize) {
        super(tickMs, ringSize);
        this.currentWorker = currentWorker;
    }

    @Override
    protected boolean verify(ExecuteTaskParam param) {
        Objects.requireNonNull(param, "Execute task param cannot be null.");
        Objects.requireNonNull(param.getWorker(), "Execute task worker cannot be null.");
        if (!currentWorker.sameWorker(param.getWorker())) {
            log.error("Take unmatched worker task: {}, {}, {}", param.getTaskId(), Worker.current(), param.getWorker());
            return false;
        }
        if (!currentWorker.getWorkerId().equals(param.getWorker().getWorkerId())) {
            log.warn("Take former worker task: {}, {}, {}", param.getTaskId(), currentWorker, param.getWorker());
            param.setWorker(currentWorker);
        }
        return true;
    }

}

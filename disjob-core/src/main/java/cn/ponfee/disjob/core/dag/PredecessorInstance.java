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

package cn.ponfee.disjob.core.dag;

import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.core.enums.ExecuteState;
import cn.ponfee.disjob.core.enums.RunState;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.core.model.SchedWorkflow;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * Workflow dag predecessor instance
 *
 * @author Ponfee
 */
@Getter
@Setter
public class PredecessorInstance extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 422243686633743869L;

    private long instanceId;
    private String curNode;
    private RunState runState;
    private List<PredecessorTask> tasks;

    public static PredecessorInstance of(SchedWorkflow workflow, List<SchedTask> tasks) {
        PredecessorInstance instance = new PredecessorInstance();
        instance.setInstanceId(workflow.getInstanceId());
        instance.setCurNode(workflow.getCurNode());
        instance.setRunState(RunState.of(workflow.getRunState()));
        instance.setTasks(Collects.convert(tasks, PredecessorInstance::convert));
        return instance;
    }

    // ----------------------------------------------------------------------private methods

    private static PredecessorTask convert(SchedTask source) {
        PredecessorTask target = new PredecessorTask();
        target.setTaskId(source.getTaskId());
        target.setTaskNo(source.getTaskNo());
        target.setTaskCount(source.getTaskCount());
        target.setExecuteSnapshot(source.getExecuteSnapshot());
        target.setExecuteState(ExecuteState.of(source.getExecuteState()));
        return target;
    }

}

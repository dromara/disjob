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

import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.core.enums.RunState;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.core.model.SchedWorkflow;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * Workflow predecessor node
 *
 * @author Ponfee
 */
@Getter
@Setter
public class WorkflowPredecessorNode extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 422243686633743869L;

    private Long instanceId;
    private Integer sequence;
    private String curNode;
    private RunState runState;
    private List<ExecutedTask> executedTasks;

    public static WorkflowPredecessorNode of(SchedWorkflow workflow, List<SchedTask> tasks) {
        WorkflowPredecessorNode node = new WorkflowPredecessorNode();
        node.setInstanceId(workflow.getInstanceId());
        node.setSequence(workflow.getSequence());
        node.setCurNode(workflow.getCurNode());
        node.setRunState(RunState.of(workflow.getRunState()));
        node.setExecutedTasks(ExecutedTask.convert(tasks));
        return node;
    }

}

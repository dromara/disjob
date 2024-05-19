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

package cn.ponfee.disjob.core.model;

import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.core.enums.ExecuteState;
import cn.ponfee.disjob.core.enums.RunState;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Workflow predecessor node
 *
 * @author Ponfee
 */
@Getter
@Setter
public class WorkflowPredecessorNode extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 422243686633743869L;

    private long instanceId;
    private int sequence;
    private String curNode;
    private RunState runState;
    private List<ExecutedTask> executedTasks;

    public static WorkflowPredecessorNode of(SchedWorkflow workflow, List<SchedTask> tasks) {
        WorkflowPredecessorNode node = new WorkflowPredecessorNode();
        node.setInstanceId(workflow.getInstanceId());
        node.setSequence(workflow.getSequence());
        node.setCurNode(workflow.getCurNode());
        node.setRunState(RunState.of(workflow.getRunState()));
        node.setExecutedTasks(convert(tasks));
        return node;
    }

    @Getter
    @Setter
    public static class ExecutedTask extends AbstractExecutionTask {
        private static final long serialVersionUID = -4625053001297718912L;

        /**
         * 执行状态
         */
        private ExecuteState executeState;
    }

    // ----------------------------------------------------------------------private methods

    private static List<ExecutedTask> convert(List<SchedTask> tasks) {
        if (tasks == null) {
            return null;
        }
        return tasks.stream().map(WorkflowPredecessorNode::convert).collect(Collectors.toList());
    }

    private static ExecutedTask convert(SchedTask source) {
        if (source == null) {
            return null;
        }

        ExecutedTask target = new ExecutedTask();
        target.setTaskId(source.getTaskId());
        target.setTaskNo(source.getTaskNo());
        target.setTaskCount(source.getTaskCount());
        target.setExecuteSnapshot(source.getExecuteSnapshot());
        target.setExecuteState(ExecuteState.of(source.getExecuteState()));
        return target;
    }

}

/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

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

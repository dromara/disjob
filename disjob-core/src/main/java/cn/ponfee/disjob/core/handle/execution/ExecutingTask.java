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

import cn.ponfee.disjob.core.model.SchedTask;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Executing task
 *
 * @author Ponfee
 */
@Getter
@Setter
public class ExecutingTask extends AbstractExecutionTask {
    private static final long serialVersionUID = 8910065837652403459L;

    /**
     * sched_job.job_id
     */
    private Long jobId;

    /**
     * sched_instance.instance_id
     */
    private Long instanceId;

    /**
     * sched_instance.wnstance_id
     * <p>非工作流任务时值为null
     */
    private Long wnstanceId;

    /**
     * job_handler执行task的参数
     */
    private String taskParam;

    /**
     * 工作流(DAG)任务的前驱节点列表数据
     */
    private List<WorkflowPredecessorNode> workflowPredecessorNodes;

    public static ExecutingTask of(Long jobId,
                                   Long wnstanceId,
                                   SchedTask task,
                                   List<WorkflowPredecessorNode> workflowPredecessorNodes) {
        ExecutingTask executingTask = ExecutionTaskConverter.INSTANCE.toExecutingTask(task);
        executingTask.setJobId(jobId);
        executingTask.setWnstanceId(wnstanceId);
        executingTask.setWorkflowPredecessorNodes(workflowPredecessorNodes);
        return executingTask;
    }

}

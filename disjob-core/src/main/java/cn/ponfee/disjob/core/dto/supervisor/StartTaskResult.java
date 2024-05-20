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

package cn.ponfee.disjob.core.dto.supervisor;

import cn.ponfee.disjob.core.model.AbstractExecutionTask;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.core.model.WorkflowPredecessorNode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Start task result
 *
 * @author Ponfee
 */
@Setter
@Getter
public class StartTaskResult extends AbstractExecutionTask {

    private static final long serialVersionUID = 2873837797283062411L;

    /**
     * Is start successful
     */
    private boolean success;

    /**
     * Start message
     */
    private String message;

    /**
     * sched_job.job_id
     */
    private long jobId;

    /**
     * sched_instance.instance_id
     */
    private long instanceId;

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

    public static StartTaskResult failure(String message) {
        StartTaskResult res = new StartTaskResult();
        res.setSuccess(false);
        res.setMessage(message);
        return res;
    }

    public static StartTaskResult success(long jobId, Long wnstanceId, SchedTask task, List<WorkflowPredecessorNode> nodes) {
        StartTaskResult res = new StartTaskResult();
        res.setSuccess(true);
        res.setTaskId(task.getTaskId());
        res.setTaskNo(task.getTaskNo());
        res.setTaskCount(task.getTaskCount());
        res.setExecuteSnapshot(task.getExecuteSnapshot());

        res.setJobId(jobId);
        res.setInstanceId(task.getInstanceId());
        res.setWnstanceId(wnstanceId);
        res.setTaskParam(task.getTaskParam());
        res.setWorkflowPredecessorNodes(nodes);
        return res;
    }

}

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

import cn.ponfee.disjob.core.dto.supervisor.StartTaskResult;
import cn.ponfee.disjob.core.model.AbstractExecutionTask;
import cn.ponfee.disjob.core.model.WorkflowPredecessorNode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * The execution task
 *
 * @author Ponfee
 */
@Getter
@Setter
public class ExecuteTask extends AbstractExecutionTask {
    private static final long serialVersionUID = 8910065837652403459L;

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

    public static ExecuteTask of(StartTaskResult source) {
        if (source == null) {
            return null;
        }

        ExecuteTask target = new ExecuteTask();
        target.setTaskId(source.getTaskId());
        target.setTaskNo(source.getTaskNo());
        target.setTaskCount(source.getTaskCount());
        target.setExecuteSnapshot(source.getExecuteSnapshot());

        target.setJobId(source.getJobId());
        target.setInstanceId(source.getInstanceId());
        target.setWnstanceId(source.getWnstanceId());
        target.setTaskParam(source.getTaskParam());
        target.setWorkflowPredecessorNodes(source.getWorkflowPredecessorNodes());
        return target;
    }

}

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

import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.core.dag.PredecessorInstance;
import cn.ponfee.disjob.core.model.SchedTask;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * Start task result
 *
 * @author Ponfee
 */
@Setter
@Getter
public class StartTaskResult extends ToJsonString implements Serializable {

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
     * 任务ID
     */
    private long taskId;

    /**
     * 当前任务序号(从1开始)
     */
    private int taskNo;

    /**
     * 任务总数量
     */
    private int taskCount;

    /**
     * job_handler执行task的参数
     */
    private String taskParam;

    /**
     * 保存的执行快照数据
     */
    private String executeSnapshot;

    /**
     * 工作流(DAG)任务的前驱节点实例列表
     */
    private List<PredecessorInstance> predecessorInstances;

    public static StartTaskResult failure(String message) {
        StartTaskResult result = new StartTaskResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

    public static StartTaskResult success(long jobId, Long wnstanceId, SchedTask task,
                                          List<PredecessorInstance> predecessorInstances) {
        StartTaskResult result = new StartTaskResult();
        result.setSuccess(true);
        result.setTaskId(task.getTaskId());
        result.setTaskNo(task.getTaskNo());
        result.setTaskCount(task.getTaskCount());
        result.setExecuteSnapshot(task.getExecuteSnapshot());

        result.setJobId(jobId);
        result.setInstanceId(task.getInstanceId());
        result.setWnstanceId(wnstanceId);
        result.setTaskParam(task.getTaskParam());
        result.setPredecessorInstances(predecessorInstances);
        return result;
    }

}

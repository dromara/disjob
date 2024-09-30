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

package cn.ponfee.disjob.worker.executor;

import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.core.enums.JobType;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * The execution task
 *
 * @author Ponfee
 */
@Getter
@Setter
public class ExecutionTask extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 8910065837652403459L;

    /**
     * sched_job.job_id
     */
    private long jobId;

    /**
     * sched_job.job_type
     */
    private JobType jobType;

    /**
     * sched_job.retry_count
     * <p>最大可重试次数
     */
    private int retryCount;

    /**
     * sched_instance.retried_count
     * <p>当前是第几次重试，如果当前非重试执行，则为0
     */
    private int retriedCount;

    /**
     * 是否广播任务
     */
    private boolean broadcast;

    /**
     * sched_instance.wnstance_id
     * <p>非工作流任务时值为null
     */
    private Long wnstanceId;

    /**
     * sched_instance.instance_id
     */
    private long instanceId;

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
     * job_executor执行task的参数
     */
    private String taskParam;

    /**
     * 保存的执行快照数据
     */
    private String executeSnapshot;

}

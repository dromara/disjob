/*
 * Copyright 2022-2026 Ponfee (http://www.ponfee.cn/)
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

package cn.ponfee.disjob.supervisor.model;

import cn.ponfee.disjob.common.dag.DAGNode;
import cn.ponfee.disjob.common.model.BaseEntity;
import cn.ponfee.disjob.core.enums.RunStatus;
import cn.ponfee.disjob.core.enums.RunType;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.beans.Transient;
import java.util.Date;

/**
 * The schedule instance entity, mapped database table sched_instance
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedInstance extends BaseEntity {
    private static final long serialVersionUID = -1457861792948169949L;

    /**
     * 全局唯一ID
     */
    private Long instanceId;

    /**
     * root instance_id(Retry、Depend、Workflow)
     */
    private Long rnstanceId;

    /**
     * parent instance_id(Retry、Depend、Workflow)
     */
    private Long pnstanceId;

    /**
     * sched_job.job_type为Workflow生成的lead instance_id
     */
    private Long wnstanceId;

    /**
     * sched_job.job_id
     *
     * @see SchedJob#getJobId()
     */
    private Long jobId;

    /**
     * 触发时间(毫秒时间戳)
     */
    private Long triggerTime;

    /**
     * 运行类型：1-SCHEDULE；2-DEPEND；3-RETRY；4-MANUAL(手动触发)；
     *
     * @see RunType
     */
    private Integer runType;

    /**
     * 去重键(保证trigger_time唯一)：0-SCHEDULE/MANUAL；{instance_id}-其它场景；
     */
    private Long dedupKey;

    /**
     * 是否重试中
     */
    private Boolean retrying;

    /**
     * 运行状态：10-待运行；20-运行中；30-已暂停；40-已完成；50-已取消；
     *
     * @see RunStatus
     */
    private Integer runStatus;

    /**
     * 运行开始时间
     */
    private Date runStartTime;

    /**
     * 运行结束时间
     */
    private Date runEndTime;

    /**
     * 当前是第几次重试(最大值为retry_count)
     */
    private Integer retryAttempt;

    /**
     * 工作流任务的当前节点(sched_workflow.cur_node，非工作流任务时为NULL)
     *
     * @see SchedWorkflow#getCurNode()
     */
    private String workflowCurNode;

    /**
     * 下一次的扫描时间
     */
    private Date nextScanTime;

    /**
     * 行记录版本号
     */
    private Integer version;

    public static SchedInstance of(SchedInstance parent, long instanceId, long jobId,
                                   RunType runType, long triggerTime, int retryAttempt) {
        return of(parent, parent.getWnstanceId(), instanceId, jobId, runType, triggerTime, retryAttempt);
    }

    /**
     * Creates sched instance
     *
     * @param parent       the parent instance
     * @param wnstanceId   the workflow instance id
     * @param instanceId   the instance id
     * @param jobId        the job id
     * @param runType      the run type
     * @param triggerTime  the trigger time
     * @param retryAttempt the retry attempt
     * @return SchedInstance
     */
    public static SchedInstance of(SchedInstance parent, Long wnstanceId, long instanceId,
                                   long jobId, RunType runType, long triggerTime, int retryAttempt) {
        Long rnstanceId = null, pnstanceId = null;
        if (parent != null) {
            rnstanceId = parent.obtainRootInstanceId();
            pnstanceId = parent.obtainSourceInstanceId();
        }
        SchedInstance instance = new SchedInstance();
        instance.setInstanceId(instanceId);
        instance.setRnstanceId(rnstanceId);
        instance.setPnstanceId(pnstanceId);
        instance.setWnstanceId(wnstanceId);
        instance.setJobId(jobId);
        instance.setRunType(runType.value());
        instance.setTriggerTime(triggerTime);
        instance.setRetryAttempt(retryAttempt);
        instance.setRunStatus(RunStatus.WAITING.value());
        return instance;
    }

    /**
     * Obtain root instance id
     *
     * @return root instance id
     */
    public long obtainRootInstanceId() {
        if (rnstanceId != null) {
            return rnstanceId;
        }
        return pnstanceId != null ? pnstanceId : instanceId;
    }

    public long obtainSourceInstanceId() {
        return isRetry() ? pnstanceId : instanceId;
    }

    public int obtainRetryAttempt() {
        return retryAttempt != null ? retryAttempt : 0;
    }

    @Transient
    public boolean isWorkflow() {
        return wnstanceId != null;
    }

    @Transient
    public boolean isWorkflowNode() {
        return isWorkflow() && !wnstanceId.equals(instanceId);
    }

    @Transient
    public boolean isWorkflowLead() {
        return isWorkflow() && wnstanceId.equals(instanceId);
    }

    @Transient
    public boolean isRunning() {
        return RunStatus.RUNNING.equalsValue(runStatus);
    }

    @Transient
    public boolean isPausable() {
        return RunStatus.of(runStatus).isPausable();
    }

    @Transient
    public boolean isPaused() {
        return RunStatus.PAUSED.equalsValue(runStatus);
    }

    @Transient
    public boolean isCompleted() {
        return RunStatus.COMPLETED.equalsValue(runStatus);
    }

    @Transient
    public boolean isTerminal() {
        return RunStatus.of(runStatus).isTerminal();
    }

    /**
     * 当前实例是否为重试实例：true-是重试实例；false-非重试实例；
     *
     * @return {@code true} if current is retry instance
     */
    @Transient
    public boolean isRetry() {
        return RunType.RETRY.equalsValue(runType);
    }

    @Transient
    public boolean isSchedule() {
        return RunType.SCHEDULE.equalsValue(runType);
    }

    public DAGNode parseWorkflowCurNode() {
        return StringUtils.isBlank(workflowCurNode) ? null : DAGNode.fromString(workflowCurNode);
    }

    public void fillDedupKey() {
        boolean isUnique = RunType.of(runType).isDedupByTriggerTime();
        // Workflow node trigger time is not unique
        this.dedupKey = (isUnique && !isWorkflowNode()) ? RunType.DEDUP_KEY_VALUE : instanceId;
    }

    public void markTerminated(RunStatus runStatus, Date runEndTime) {
        Assert.state(runStatus.isTerminal(), () -> "Invalid terminal run status: " + instanceId + ", " + runStatus);
        Assert.state(runEndTime != null, () -> "Run end time cannot be null: " + instanceId);
        this.runStatus = runStatus.value();
        this.runEndTime = runEndTime;
    }

}

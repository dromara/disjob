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

package cn.ponfee.disjob.supervisor.model;

import cn.ponfee.disjob.common.model.BaseEntity;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.enums.RunState;
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
     * 唯一标识(保证trigger_time唯一)：0-SCHEDULE/MANUAL；{instance_id}-其它场景；
     */
    private Long uniqueFlag;

    /**
     * 是否重试中
     */
    private Boolean retrying;

    /**
     * 运行状态：10-待运行；20-运行中；30-已暂停；40-已完成；50-已取消；
     *
     * @see RunState
     */
    private Integer runState;

    /**
     * 运行开始时间
     */
    private Date runStartTime;

    /**
     * 运行结束时间
     */
    private Date runEndTime;

    /**
     * 已重试的次数(the maximum value is sched_job.retry_count)
     */
    private Integer retriedCount;

    /**
     * 附加信息
     *
     * @see InstanceAttach
     */
    private String attach;

    /**
     * 下一次的扫描时间
     */
    private Date nextScanTime;

    /**
     * 行记录版本号
     */
    private Integer version;

    public static SchedInstance of(SchedInstance parent, long instanceId, long jobId,
                                   RunType runType, long triggerTime, int retriedCount) {
        return of(parent, parent.getWnstanceId(), instanceId, jobId, runType, triggerTime, retriedCount);
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
     * @param retriedCount the retried count
     * @return SchedInstance
     */
    public static SchedInstance of(SchedInstance parent, Long wnstanceId, long instanceId,
                                   long jobId, RunType runType, long triggerTime, int retriedCount) {
        Long rnstanceId = null, pnstanceId = null;
        if (parent != null) {
            rnstanceId = parent.obtainRnstanceId();
            pnstanceId = parent.obtainRetryOriginalInstanceId();
        }
        SchedInstance instance = new SchedInstance();
        instance.setInstanceId(instanceId);
        instance.setRnstanceId(rnstanceId);
        instance.setPnstanceId(pnstanceId);
        instance.setWnstanceId(wnstanceId);
        instance.setJobId(jobId);
        instance.setRunType(runType.value());
        instance.setTriggerTime(triggerTime);
        instance.setRetriedCount(retriedCount);
        instance.setRunState(RunState.WAITING.value());
        return instance;
    }

    /**
     * Obtain root instance id
     *
     * @return root instance id
     */
    public long obtainRnstanceId() {
        if (rnstanceId != null) {
            return rnstanceId;
        }
        return pnstanceId != null ? pnstanceId : instanceId;
    }

    public long obtainRetryOriginalInstanceId() {
        return isRunRetry() ? pnstanceId : instanceId;
    }

    public int obtainRetriedCount() {
        return retriedCount != null ? retriedCount : 0;
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
        return RunState.RUNNING.equalsValue(runState);
    }

    @Transient
    public boolean isPaused() {
        return RunState.PAUSED.equalsValue(runState);
    }

    @Transient
    public boolean isCompleted() {
        return RunState.COMPLETED.equalsValue(runState);
    }

    @Transient
    public boolean isTerminal() {
        return RunState.of(runState).isTerminal();
    }

    @Transient
    public boolean isRunRetry() {
        return RunType.RETRY.equalsValue(runType);
    }

    public InstanceAttach parseAttach() {
        if (StringUtils.isBlank(attach)) {
            return null;
        }
        return Jsons.fromJson(attach, InstanceAttach.class);
    }

    public SchedInstance fillUniqueFlag() {
        RunType type = RunType.of(runType);
        // Workflow node trigger time is not unique
        this.uniqueFlag = (type.isUniqueFlag() && !isWorkflowNode()) ? type.getUniqueFlag() : instanceId;
        return this;
    }

    public void markTerminated(RunState runState, Date runEndTime) {
        Assert.state(runState.isTerminal(), () -> "Invalid terminal run state: " + instanceId + ", " + runState);
        Assert.state(runEndTime != null, () -> "Run end time cannot be null: " + instanceId);
        this.runState = runState.value();
        this.runEndTime = runEndTime;
    }

}

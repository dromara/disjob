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
import cn.ponfee.disjob.core.base.CoreUtils;
import cn.ponfee.disjob.core.enums.*;
import com.google.common.math.IntMath;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.beans.Transient;
import java.util.Date;

import static cn.ponfee.disjob.common.date.Dates.format;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * The schedule job entity, mapped database table sched_job
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedJob extends BaseEntity {
    private static final long serialVersionUID = -1595287180906247896L;

    /**
     * 全局唯一ID
     */
    private Long jobId;

    /**
     * 分组名称(可以理解为一个应用的appid，此job只会分派给所属组的Worker执行)
     */
    private String group;

    /**
     * Job名称
     */
    private String jobName;

    /**
     * Job类型：1-常规；2-工作流(DAG)；
     *
     * @see JobType
     */
    private Integer jobType;

    /**
     * Job状态：0-禁用；1-启用；
     *
     * @see JobState
     */
    private Integer jobState;

    /**
     * Job执行器(支持：执行器类的全限定名、Spring bean name、DAG表达式、执行器源码等)
     */
    private String jobExecutor;

    /**
     * Job参数
     */
    private String jobParam;

    /**
     * 调度失败重试类型：0-不重试；1-只重试失败的Task；2-重试所有的Task；
     *
     * @see RetryType
     */
    private Integer retryType;

    /**
     * 调度失败可重试的最大次数
     */
    private Integer retryCount;

    /**
     * 调度失败重试间隔(毫秒)，阶梯递增(square of sched_instance.retried_count)
     */
    private Integer retryInterval;

    /**
     * Job有效起始时间(为空不限制)
     */
    private Date startTime;

    /**
     * Job有效终止时间(为空不限制)
     */
    private Date endTime;

    /**
     * 触发器类型：1-Cron表达式；2-指定时间；3-指定周期；4-指定间隔；5-固定频率；6-固定延时；7-任务依赖；
     *
     * @see TriggerType
     */
    private Integer triggerType;

    /**
     * 触发器值(对应trigger_type)：1-Cron表达式；2-时间格式(2000-01-01 00:00:00)；3-{"period":"MONTHLY","start":"2000-01-01 00:00:00","step":1}；4-指定间隔秒数；5-固定频率秒数；6-固定延时秒数；7-父任务job_id(多个逗号分隔)；
     */
    private String triggerValue;

    /**
     * 执行超时时间(毫秒)，若大于0则执行超时会中断任务
     */
    private Integer executeTimeout;

    /**
     * 冲突策略(如果上一次调度未完成，下一次调度执行策略)：1-并发执行；2-顺序执行；3-覆盖上次任务（取消上次任务，执行本次任务）；4-丢弃本次任务；
     *
     * @see CollidedStrategy
     */
    private Integer collidedStrategy;

    /**
     * 过期策略：1-立即触发执行一次；2-跳过所有被错过的；3-执行所有被错过的；
     *
     * @see MisfireStrategy
     */
    private Integer misfireStrategy;

    /**
     * 任务分派给哪一个worker的路由策略：1-轮询；2-随机；3-简单的哈希；4-一致性哈希；5-本地优先；6-广播；
     *
     * @see RouteStrategy
     */
    private Integer routeStrategy;

    /**
     * Worker关机的执行策略(如重新发布服务时)：1-恢复执行；2-暂停执行；3-取消执行；
     *
     * @see ShutdownStrategy
     */
    private Integer shutdownStrategy;

    /**
     * 最近一次的触发时间(毫秒时间戳)
     */
    private Long lastTriggerTime;

    /**
     * 下一次的触发时间(毫秒时间戳)
     */
    private Long nextTriggerTime;

    /**
     * 下一次的扫描时间
     */
    private Date nextScanTime;

    /**
     * 连续失败的扫描次数，连续失败次数达到阈值后自动禁用(set job_state=0)
     */
    private Integer scanFailedCount;

    /**
     * 备注
     */
    private String remark;

    /**
     * 行记录版本号
     */
    private Integer version;

    /**
     * 更新人
     */
    private String updatedBy;

    /**
     * 创建人
     */
    private String createdBy;

    @Transient
    public boolean isEnabled() {
        return JobState.ENABLED.equalsValue(jobState);
    }

    @Transient
    public boolean isDisabled() {
        return JobState.DISABLED.equalsValue(jobState);
    }

    @Transient
    public boolean isFixedTriggerType() {
        return TriggerType.of(triggerType).isFixedTriggerType();
    }

    @Transient
    public boolean isBroadcast() {
        return RouteStrategy.of(routeStrategy).isBroadcast();
    }

    public Long obtainNextTriggerTime() {
        if (nextTriggerTime == null || endTime == null) {
            return nextTriggerTime;
        }
        return nextTriggerTime > endTime.getTime() ? null : nextTriggerTime;
    }

    public void verifyForAdd(int maximumJobRetryCount) {
        this.group = CoreUtils.trimRequired(group, 60, "Group");
        verifyAndDefaultSetting(maximumJobRetryCount);
    }

    public void verifyForUpdate(int maximumJobRetryCount) {
        Assert.isTrue(jobId != null && jobId > 0, () -> "Invalid jobId: " + jobId);
        Assert.isTrue(version != null && version > 0, () -> "Invalid version: " + version);
        verifyAndDefaultSetting(maximumJobRetryCount);
    }

    public boolean requiredUpdateExecutor() {
        if (jobExecutor == null) {
            Assert.isNull(jobParam, "Job param must be null when not set job executor.");
            Assert.isNull(jobType, "Job type must be null when not set job executor.");
            Assert.isNull(routeStrategy, "Route strategy must be null when not set job executor.");
            return false;
        }
        return true;
    }

    public boolean requiredUpdateTrigger(Integer curTriggerType, String curTriggerValue) {
        if (triggerType == null) {
            Assert.isNull(triggerValue, "Trigger value must be null when not set trigger type.");
            return false;
        }
        if (curTriggerType.equals(triggerType) && curTriggerValue.equals(triggerValue)) {
            // needless update
            this.triggerType = null;
            this.triggerValue = null;
            return false;
        }
        return true;
    }

    public int incrementAndGetScanFailedCount() {
        return ++this.scanFailedCount;
    }

    public boolean retryable(RunState runState, int retriedCount) {
        Assert.state(runState.isTerminal(), "Run state must be terminated.");
        if (!runState.isFailure()) {
            return false;
        }
        return !RetryType.NONE.equalsValue(retryType) && retriedCount < retryCount;
    }

    /**
     * Returns the retry trigger time
     *
     * @param failCount the failure times
     * @return retry trigger time milliseconds
     */
    public long computeRetryTriggerTime(int failCount) {
        Assert.isTrue(!RetryType.NONE.equalsValue(retryType), () -> "Sched job '" + jobId + "' retry type is NONE.");
        Assert.isTrue(retryCount > 0, () -> "Sched job '" + jobId + "' retry count must greater than 0, but actual " + retryCount);
        Assert.isTrue(failCount <= retryCount, () -> "Sched job '" + jobId + "' retried " + failCount + " exceed " + retryCount + " limit.");
        // exponential backoff
        return System.currentTimeMillis() + (long) retryInterval * IntMath.pow(failCount, 2);
    }

    // ----------------------------------------------------------------private methods

    private void verifyAndDefaultSetting(int maximumJobRetryCount) {
        this.jobName = CoreUtils.trimRequired(jobName, 60, "Job name");
        this.remark = CoreUtils.trimOptional(remark, 255, "Remark");

        // set default
        this.jobState = defaultIfNull(jobState, JobState.DISABLED.value());
        this.retryType = defaultIfNull(retryType, RetryType.NONE.value());
        this.executeTimeout = defaultIfNull(executeTimeout, 0);
        this.collidedStrategy = defaultIfNull(collidedStrategy, CollidedStrategy.CONCURRENT.value());
        this.misfireStrategy = defaultIfNull(misfireStrategy, MisfireStrategy.FIRE_ONCE_NOW.value());
        this.shutdownStrategy = defaultIfNull(shutdownStrategy, ShutdownStrategy.RESUME.value());
        this.triggerValue = StringUtils.trim(triggerValue);

        // verify
        JobState.of(jobState);
        Assert.isTrue(executeTimeout >= 0, () -> "Invalid execute timeout: " + executeTimeout);
        CollidedStrategy.of(collidedStrategy);
        MisfireStrategy.of(misfireStrategy);
        ShutdownStrategy.of(shutdownStrategy);
        if (RetryType.of(retryType) == RetryType.NONE) {
            this.retryCount = defaultIfNull(retryCount, 0);
            this.retryInterval = defaultIfNull(retryInterval, 0);
            Assert.isTrue(retryCount == 0, "Retry count cannot set value.");
            Assert.isTrue(retryInterval == 0, "Retry interval cannot set value.");
        } else {
            boolean verify = (retryCount != null && 0 < retryCount && retryCount <= maximumJobRetryCount);
            Assert.isTrue(verify, () -> "Retry count must be range [1, " + maximumJobRetryCount + "]");
            Assert.isTrue(retryInterval != null && retryInterval > 0, "Retry interval must greater than 0.");
        }
        if (startTime != null && endTime != null && startTime.after(endTime)) {
            throw new IllegalArgumentException("Invalid time range: [" + format(startTime) + " ~ " + format(endTime) + "]");
        }
    }

}

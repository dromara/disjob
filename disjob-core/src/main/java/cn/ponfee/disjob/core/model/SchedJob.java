/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.model;

import cn.ponfee.disjob.common.model.BaseEntity;
import cn.ponfee.disjob.core.enums.*;
import com.google.common.math.IntMath;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Date;

import static cn.ponfee.disjob.common.date.Dates.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.length;

/**
 * The schedule job entity, mapped database table sched_job
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedJob extends BaseEntity implements Serializable {

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
     * Job处理器(支持：处理器类的全限定名、Spring bean name、DAG表达式、处理器源码等)
     */
    private String jobHandler;

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
     * Job起始时间(为空不限制)
     */
    private Date startTime;

    /**
     * Job结束时间(为空不限制)
     */
    private Date endTime;

    /**
     * 触发器类型：1-Cron表达式；2-指定时间；3-固定周期；4-固定频率；5-固定延时；6-任务依赖；
     *
     * @see TriggerType
     */
    private Integer triggerType;

    /**
     * 触发器值(对应trigger_type)：1-Cron表达式；2-时间格式(2000-01-01 00:00:00)；3-{"period":"DAILY","start":"2018-12-06 00:00:00","step":1}；4-周期秒数；5-延时秒数；6-父任务job_id(多个逗号分隔)；
     */
    private String triggerValue;

    /**
     * 执行超时时间(毫秒)，若大于0则执行超时会中断任务
     */
    private Integer executeTimeout;

    /**
     * 冲突策略(如果上一次调度未完成，下一次调度执行策略)：1-并行执行；2-串行执行；3-覆盖上次任务（取消上次任务，执行本次任务）；4-丢弃本次任务；
     *
     * @see CollidedStrategy
     */
    private Integer collidedStrategy;

    /**
     * 过期策略：1-触发最近一次；2-丢弃；3-触发所有；
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
    private Integer failedScanCount;

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

    public Long obtainNextTriggerTime() {
        if (nextTriggerTime == null || endTime == null) {
            return nextTriggerTime;
        }
        return nextTriggerTime > endTime.getTime() ? null : nextTriggerTime;
    }

    public void verifyBeforeAdd() {
        TriggerType type = TriggerType.of(triggerType);
        if (!type.validate(triggerValue)) {
            throw new IllegalArgumentException("Invalid trigger value: " + triggerType + ", " + triggerValue);
        }
        Assert.isTrue(length(triggerValue) <= 255, "triggerValue length cannot exceed 255.");
        Assert.isTrue(isNotBlank(group), "Group cannot be blank.");
        Assert.isTrue(length(group) <= 30, "Group length cannot exceed 30.");
        Assert.isTrue(isNotBlank(jobName), "jobName cannot be blank.");
        Assert.isTrue(length(jobName) <= 60, "jobName length cannot exceed 60.");
        Assert.hasText(jobHandler, "Job handler cannot be empty.");
        Assert.isTrue(length(remark) <= 255, "remark length cannot exceed 255.");

        this.triggerValue = triggerValue.trim();
        this.group = group.trim();
        this.jobName = jobName.trim();
        this.jobHandler = jobHandler.trim();
        this.remark = (remark == null) ? "" : remark.trim();
    }

    public void verifyBeforeUpdate() {
        Assert.isTrue(jobId != null && jobId > 0, () -> "Invalid jobId: " + jobId);
        Assert.isTrue(version != null && version > 0, () -> "Invalid version: " + version);
        verifyBeforeAdd();
    }

    public void checkAndDefaultSetting() {
        if (jobState == null) {
            this.jobState = JobState.DISABLE.value();
        }
        if (jobType == null) {
            this.jobType = JobType.GENERAL.value();
        }

        if (retryType == null) {
            this.retryType = RetryType.NONE.value();
        }
        if (RetryType.of(retryType) == RetryType.NONE) {
            if (retryCount == null) {
                this.retryCount = 0;
            }
            if (retryInterval == null) {
                this.retryInterval = 0;
            }
            Assert.isTrue(retryCount == 0, "Retry count cannot set if none retry.");
            Assert.isTrue(retryInterval == 0, "Retry interval cannot set if none retry.");
        } else {
            Assert.isTrue(retryCount != null && retryCount > 0, "Retry count must set if retry.");
            Assert.isTrue(retryInterval != null && retryInterval > 0, "Retry interval must set if retry.");
        }

        if (executeTimeout == null) {
            this.executeTimeout = 0;
        }
        if (collidedStrategy == null) {
            this.collidedStrategy = CollidedStrategy.CONCURRENT.value();
        }
        if (misfireStrategy == null) {
            this.misfireStrategy = MisfireStrategy.LAST.value();
        }
        if (routeStrategy == null) {
            this.routeStrategy = RouteStrategy.ROUND_ROBIN.value();
        }

        // verify
        JobState.of(jobState);
        JobType.of(jobType);
        Assert.isTrue(executeTimeout >= 0, () -> "Invalid execute timeout: " + executeTimeout);
        CollidedStrategy.of(collidedStrategy);
        MisfireStrategy.of(misfireStrategy);
        RouteStrategy.of(routeStrategy);
        if (startTime != null && endTime != null && startTime.after(endTime)) {
            throw new IllegalArgumentException("Invalid time range: [" + format(startTime) + " ~ " + format(endTime) + "]");
        }
        if (jobParam == null) {
            this.jobParam = "";
        }
    }

    public int incrementAndGetFailedScanCount() {
        return ++this.failedScanCount;
    }

    public boolean retryable(RunState runState, int retriedCount) {
        Assert.state(runState.isTerminal(), "Run state must be terminated.");
        if (!runState.isFailure()) {
            return false;
        }
        return !RetryType.NONE.equals(retryType) && retriedCount < retryCount;
    }

    /**
     * Returns the retry trigger time
     *
     * @param failCount the failure times
     * @param current   the current date time
     * @return retry trigger time milliseconds
     */
    public long computeRetryTriggerTime(int failCount, Date current) {
        Assert.isTrue(!RetryType.NONE.equals(retryType), () -> "Sched job '" + jobId + "' retry type is NONE.");
        Assert.isTrue(retryCount > 0, () -> "Sched job '" + jobId + "' retry count must greater than 0, but actual " + retryCount);
        Assert.isTrue(failCount <= retryCount, () -> "Sched job '" + jobId + "' retried " + failCount + " exceed " + retryCount + " limit.");
        // exponential backoff
        return current.getTime() + (long) retryInterval * IntMath.pow(failCount, 2);
    }

}

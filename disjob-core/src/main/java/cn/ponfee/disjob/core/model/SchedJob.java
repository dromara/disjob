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
     * Job分组(用于分配给同组下的Worker执行)
     */
    private String jobGroup;

    /**
     * Job名称
     */
    private String jobName;

    /**
     * Job处理器(实现处理器接口类的全限定名、Spring bean name、DAG表达式、源码)
     */
    private String jobHandler;

    /**
     * Job状态：0-已禁用；1-已启用；
     *
     * @see JobState
     */
    private Integer jobState;

    /**
     * Job类型：1-普通(Normal)；2-工作流(Workflow)；
     *
     * @see JobType
     */
    private Integer jobType;

    /**
     * Job参数
     */
    private String jobParam;

    /**
     * 调度失败重试类型：0-不重试；1-重试所有的Task；2-只重试失败的Task；
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
     * 触发器类型：1-Crontab方式；2-指定时间执行一次；3-周期性执行；4-任务依赖；
     *
     * @see TriggerType
     */
    private Integer triggerType;

    /**
     * 触发器配置(对应trigger_type)：1-Crontab表达式；2-时间格式；3-{"period":"DAILY","start":"2018-12-06 00:00:00","step":1}；4-父任务job_id(多个逗号分隔)；
     */
    private String triggerValue;

    /**
     * 执行超时时间(毫秒)，若大于0则执行超时会中断任务
     */
    private Integer executeTimeout;

    /**
     * 冲突策略(如果上一次调度未完成，下一次调度执行策略)：1-并行；2-串行；3-覆盖(先取消上一次的执行)；4-丢弃；
     *
     * @see CollisionStrategy
     */
    private Integer collisionStrategy;

    /**
     * 过期策略：1-触发最近一次；2-丢弃；3-触发所有；
     *
     * @see MisfireStrategy
     */
    private Integer misfireStrategy;

    /**
     * 任务分配给哪一个worker的路由策略：1-轮询；2-随机；3-简单的哈希；4-一致性哈希；5-本地优先；6-广播；
     *
     * @see RouteStrategy
     */
    private Integer routeStrategy;

    /**
     * Job的权重分数，用于分配各Job的调度资源(分数越高表示需要占用的资源越多)
     */
    private Integer weightScore;

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
     * 告警订阅人员列表
     */
    private String alarmSubscribers;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否已删除：0-否；NULL-是(用NULL来解决因软删引起的唯一索引冲突问题)；
     */
    private Boolean deleted;

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

    public void verifyBeforeAdd() {
        Assert.notNull(triggerType, "triggerType cannot be null.");
        Assert.notNull(triggerValue, "triggerValue cannot be null.");
        Assert.isTrue(length(triggerValue) <= 255, "triggerValue length cannot exceed 255.");
        Assert.isTrue(isNotBlank(jobGroup), "jobGroup cannot be blank.");
        Assert.isTrue(length(jobGroup) <= 30, "jobGroup length cannot exceed 30.");
        Assert.isTrue(isNotBlank(jobName), "jobName cannot be blank.");
        Assert.isTrue(length(jobName) <= 60, "jobName length cannot exceed 60.");
        Assert.isTrue(length(alarmSubscribers) <= 512, "alarmSubscribers length cannot exceed 512.");
        Assert.isTrue(length(remark) <= 255, "remark length cannot exceed 255.");
    }

    public void verifyBeforeUpdate() {
        Assert.isTrue(jobId != null && jobId > 0, () -> "Invalid jobId: " + jobId);
        Assert.isTrue(getVersion() != null && getVersion() > 0, () -> "Invalid version: " + getVersion());
        verifyBeforeAdd();
    }

    public void checkAndDefaultSetting() {
        if (jobState == null) {
            this.jobState = JobState.DISABLE.value();
        }
        if (jobType == null) {
            this.jobType = JobType.NORMAL.value();
        }
        if (weightScore == null) {
            this.weightScore = 1;
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
        if (collisionStrategy == null) {
            this.collisionStrategy = CollisionStrategy.CONCURRENT.value();
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
        Assert.isTrue(weightScore > 0, () -> "Invalid weight score: " + weightScore);
        Assert.isTrue(executeTimeout >= 0, () -> "Invalid execute timeout: " + executeTimeout);
        CollisionStrategy.of(collisionStrategy);
        MisfireStrategy.of(misfireStrategy);
        RouteStrategy.of(routeStrategy);
        if (startTime != null && endTime != null) {
            Assert.isTrue(!startTime.after(endTime), () -> "Invalid time: " + format(startTime) + ">=" + format(endTime));
        }
        if (jobParam == null) {
            this.jobParam = "";
        }
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

package cn.ponfee.scheduler.core.model;

import cn.ponfee.scheduler.common.base.model.BaseEntity;
import cn.ponfee.scheduler.common.date.Dates;
import cn.ponfee.scheduler.core.enums.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Date;

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
     * Job处理器(实现处理器接口的全限定类名或源代码)
     */
    private String jobHandler;

    /**
     * Job状态：0-已停止；1-已启动；
     *
     * @see JobState
     */
    private Integer jobState;

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
     * 调度失败重试间隔(毫秒)，阶梯递增(square of sched_track.retried_count)
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
    private String triggerConf;

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
     * 任务分配给哪一个worker的路由策：1-轮询；2-随机；3-简单的哈希；4-一致性哈希；5-本地优先；
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
     * 是否已删除
     */
    private Boolean deleted;

    /**
     * 更新人
     */
    private String updatedBy;

    /**
     * 创建人
     */
    private String createdBy;

    public void defaultSettingAndVerify() {
        if (jobState == null) {
            this.jobState = JobState.STOPPED.value();
        }
        if (weightScore == null) {
            this.weightScore = 1;
        }

        if (retryType == null) {
            this.retryType = RetryType.NONE.value();
        }
        RetryType retryType0 = RetryType.of(getRetryType());
        if (retryType0 == RetryType.NONE) {
            if (retryCount == null) {
                this.retryCount = 0;
            }
            if (retryInterval == null) {
                this.retryInterval = 0;
            }
            Assert.isTrue(retryCount == 0, "Retry count cannot set if none retry.");
            Assert.isTrue(retryInterval == 0, "Retry interval cannot set if none retry.");
        } else {
            Assert.isTrue(retryCount != null || retryCount > 0, "Retry count must set if retry.");
            Assert.isTrue(retryInterval != null || retryInterval > 0, "Retry interval must set if retry.");
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
        Assert.isTrue(weightScore > 0, "Invalid weight score: " + weightScore);
        Assert.isTrue(executeTimeout >= 0, "Invalid execute timeout: " + executeTimeout);
        CollisionStrategy.of(collisionStrategy);
        MisfireStrategy.of(misfireStrategy);
        RouteStrategy.of(routeStrategy);
        if (startTime != null && endTime != null) {
            Assert.isTrue(
                startTime.before(endTime),
                () -> "Invalid job time param: " + Dates.format(startTime) + ">=" + Dates.format(endTime)
            );
        }
        if (jobParam == null) {
            this.jobParam = "";
        }
    }

}

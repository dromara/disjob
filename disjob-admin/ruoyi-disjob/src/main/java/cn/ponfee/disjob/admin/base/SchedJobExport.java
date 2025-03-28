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

package cn.ponfee.disjob.admin.base;

import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.supervisor.application.response.SchedJobResponse;
import com.ruoyi.common.annotation.Excel;
import org.springframework.beans.BeanUtils;

import java.util.Date;

/**
 * SchedJob 导出
 *
 * @author Ponfee
 */
public class SchedJobExport {

    /**
     * 全局唯一ID
     */
    @Excel(name = "JobId")
    private Long jobId;

    /**
     * 分组名称(可以理解为一个应用的appid，此job只会分派给所属组的Worker执行)
     */
    @Excel(name = "分组名称")
    private String group;

    /**
     * Job名称
     */
    @Excel(name = "Job名称")
    private String jobName;

    /**
     * Job类型：1-常规；2-工作流(DAG)；
     */
    @Excel(name = "Job类型")
    private Integer jobType;

    /**
     * Job状态：0-禁用；1-启用；
     */
    @Excel(name = "Job状态")
    private Integer jobState;

    /**
     * Job执行器(支持：执行器类的全限定名、Spring bean name、DAG表达式、执行器源码等)
     */
    @Excel(name = "Job执行器")
    private String jobExecutor;

    /**
     * Job参数
     */
    @Excel(name = "Job参数")
    private String jobParam;

    /**
     * 实例失败的重试类型：0-不重试；1-只重试失败的Task；2-重试所有的Task；
     */
    @Excel(name = "重试类型")
    private Integer retryType;

    /**
     * 实例失败的最大重试次数
     */
    @Excel(name = "最大重试次数")
    private Integer retryCount;

    /**
     * 实例失败的重试间隔(毫秒)，阶梯递增(square of sched_instance.retried_count)
     */
    @Excel(name = "重试间隔(毫秒)")
    private Integer retryInterval;

    /**
     * Job有效起始时间(为空不限制)
     */
    @Excel(name = "Job起始时间", width = 30, dateFormat = Dates.DATETIME_PATTERN)
    private Date startTime;

    /**
     * Job有效终止时间(为空不限制)
     */
    @Excel(name = "Job结束时间", width = 30, dateFormat = Dates.DATETIME_PATTERN)
    private Date endTime;

    /**
     * 触发器类型：1-Cron表达式；2-指定时间；3-指定周期；4-指定间隔；5-固定频率；6-固定延时；7-任务依赖；
     */
    @Excel(name = "触发器类型")
    private Integer triggerType;

    /**
     * 触发器值(对应trigger_type)：1-Cron表达式；2-时间格式(2000-01-01 00:00:00)；3-{"period":"MONTHLY","start":"2000-01-01 00:00:00","step":1}；4-指定间隔秒数；5-固定频率秒数；6-固定延时秒数；7-父任务job_id(多个逗号分隔)；
     */
    @Excel(name = "触发器值")
    private String triggerValue;

    /**
     * 执行超时时间(毫秒)，若大于0则执行超时会中断任务
     */
    @Excel(name = "执行超时时间(毫秒)")
    private Integer executeTimeout;

    /**
     * 冲突策略(如果上一次调度未完成，下一次调度执行策略)：1-并发执行；2-顺序执行；3-覆盖上次任务（取消上次任务，执行本次任务）；4-丢弃本次任务；
     */
    @Excel(name = "冲突策略")
    private Integer collidedStrategy;

    /**
     * 过期策略：1-立即触发执行一次；2-跳过所有被错过的；3-执行所有被错过的；
     */
    @Excel(name = "过期策略")
    private Integer misfireStrategy;

    /**
     * 任务分派给哪一个worker的路由策略：1-轮询；2-随机；3-简单的哈希；4-一致性哈希；5-本地优先；6-广播；
     */
    @Excel(name = "路由策略")
    private Integer routeStrategy;

    /**
     * Worker关机的执行策略(如重新发布服务时)：1-恢复执行；2-暂停执行；3-取消执行；
     */
    @Excel(name = "Worker关机的执行策略")
    private Integer shutdownStrategy;

    /**
     * 告警选项(存储位运算`xor`的结果)：1-警报；2-通知；3-全选；
     */
    @Excel(name = "告警选项")
    private Integer alertOptions;

    public static SchedJobExport of(SchedJobResponse schedJobResponse) {
        SchedJobExport schedJobExport = new SchedJobExport();
        BeanUtils.copyProperties(schedJobResponse, schedJobExport);
        return schedJobExport;
    }

    // ------------------------------------------------------------getter/setter

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public Integer getJobType() {
        return jobType;
    }

    public void setJobType(Integer jobType) {
        this.jobType = jobType;
    }

    public Integer getJobState() {
        return jobState;
    }

    public void setJobState(Integer jobState) {
        this.jobState = jobState;
    }

    public String getJobExecutor() {
        return jobExecutor;
    }

    public void setJobExecutor(String jobExecutor) {
        this.jobExecutor = jobExecutor;
    }

    public String getJobParam() {
        return jobParam;
    }

    public void setJobParam(String jobParam) {
        this.jobParam = jobParam;
    }

    public Integer getRetryType() {
        return retryType;
    }

    public void setRetryType(Integer retryType) {
        this.retryType = retryType;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(Integer retryInterval) {
        this.retryInterval = retryInterval;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Integer getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(Integer triggerType) {
        this.triggerType = triggerType;
    }

    public String getTriggerValue() {
        return triggerValue;
    }

    public void setTriggerValue(String triggerValue) {
        this.triggerValue = triggerValue;
    }

    public Integer getExecuteTimeout() {
        return executeTimeout;
    }

    public void setExecuteTimeout(Integer executeTimeout) {
        this.executeTimeout = executeTimeout;
    }

    public Integer getCollidedStrategy() {
        return collidedStrategy;
    }

    public void setCollidedStrategy(Integer collidedStrategy) {
        this.collidedStrategy = collidedStrategy;
    }

    public Integer getMisfireStrategy() {
        return misfireStrategy;
    }

    public void setMisfireStrategy(Integer misfireStrategy) {
        this.misfireStrategy = misfireStrategy;
    }

    public Integer getRouteStrategy() {
        return routeStrategy;
    }

    public void setRouteStrategy(Integer routeStrategy) {
        this.routeStrategy = routeStrategy;
    }

    public Integer getShutdownStrategy() {
        return shutdownStrategy;
    }

    public void setShutdownStrategy(Integer shutdownStrategy) {
        this.shutdownStrategy = shutdownStrategy;
    }

    public Integer getAlertOptions() {
        return alertOptions;
    }

    public void setAlertOptions(Integer alertOptions) {
        this.alertOptions = alertOptions;
    }

}

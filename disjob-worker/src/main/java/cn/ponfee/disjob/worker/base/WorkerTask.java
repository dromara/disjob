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

package cn.ponfee.disjob.worker.base;

import cn.ponfee.disjob.common.util.UuidUtils;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.dto.supervisor.StartTaskParam;
import cn.ponfee.disjob.core.dto.supervisor.StartTaskResult;
import cn.ponfee.disjob.core.dto.supervisor.StopTaskParam;
import cn.ponfee.disjob.core.enums.*;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import cn.ponfee.disjob.worker.executor.ExecutionTask;
import cn.ponfee.disjob.worker.executor.JobExecutor;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Worker task
 *
 * @author Ponfee
 */
@Getter
final class WorkerTask {

    private final long taskId;
    private final long instanceId;
    private final Long wnstanceId;
    private final long triggerTime;
    private final long jobId;
    private final int retryCount;
    private final int retriedCount;
    private final JobType jobType;
    private final RouteStrategy routeStrategy;
    private final ShutdownStrategy shutdownStrategy;
    private final int executeTimeout;
    private final String jobExecutor;
    private final Worker worker;

    /**
     * 操作类型
     */
    @Getter(AccessLevel.NONE)
    private final AtomicReference<Operation> operationRef;

    /**
     * 任务执行器
     */
    @Getter(AccessLevel.NONE)
    private final AtomicReference<JobExecutor> taskExecutorRef = new AtomicReference<>();

    WorkerTask(ExecuteTaskParam param) {
        this.operationRef = new AtomicReference<>(Objects.requireNonNull(param.getOperation()));
        this.taskId = param.getTaskId();
        this.instanceId = param.getInstanceId();
        this.wnstanceId = param.getWnstanceId();
        this.triggerTime = param.getTriggerTime();
        this.jobId = param.getJobId();
        this.retryCount = param.getRetryCount();
        this.retriedCount = param.getRetriedCount();
        this.jobType = Objects.requireNonNull(param.getJobType());
        this.routeStrategy = Objects.requireNonNull(param.getRouteStrategy());
        this.shutdownStrategy = Objects.requireNonNull(param.getShutdownStrategy());
        this.executeTimeout = param.getExecuteTimeout();
        this.jobExecutor = param.getJobExecutor();
        this.worker = Objects.requireNonNull(param.getWorker());
    }

    // --------------------------------------------------------other methods

    Long getLockInstanceId() {
        return wnstanceId != null ? wnstanceId : instanceId;
    }

    Operation getOperation() {
        return operationRef.get();
    }

    boolean updateOperation(Operation expect, Operation update) {
        return !Objects.equals(expect, update)
            && operationRef.compareAndSet(expect, update);
    }

    void bindTaskExecutor(JobExecutor executor) {
        taskExecutorRef.set(executor);
    }

    void stop() {
        JobExecutor executor = taskExecutorRef.get();
        if (executor != null) {
            executor.stop();
        }
    }

    StartTaskParam toStartTaskParam() {
        return StartTaskParam.of(jobId, wnstanceId, instanceId, taskId, jobType, worker.serialize(), UuidUtils.uuid32());
    }

    StopTaskParam toStopTaskParam(Operation ops, ExecuteState toState, String errorMsg) {
        return StopTaskParam.of(wnstanceId, instanceId, taskId, worker.serialize(), ops, toState, errorMsg);
    }

    ExecutionTask toExecutionTask(StartTaskResult source) {
        if (source == null) {
            return null;
        }

        ExecutionTask target = new ExecutionTask();
        target.setJobId(jobId);
        target.setRetryCount(retryCount);
        target.setRetriedCount(retriedCount);
        target.setBroadcast(routeStrategy.isBroadcast());
        target.setJobType(jobType);
        target.setWnstanceId(wnstanceId);
        target.setInstanceId(instanceId);
        target.setTriggerTime(new Date(triggerTime));

        target.setTaskId(source.getTaskId());
        target.setTaskNo(source.getTaskNo());
        target.setTaskCount(source.getTaskCount());
        target.setExecuteSnapshot(source.getExecuteSnapshot());
        target.setTaskParam(source.getTaskParam());

        return target;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WorkerTask)) {
            return false;
        }
        WorkerTask that = (WorkerTask) obj;
        return this.taskId == that.taskId
            && this.operationRef.get() == that.operationRef.get();
    }

    @Override
    public int hashCode() {
        return Long.hashCode(taskId) + operationRef.get().ordinal() * 31;
    }

    @Override
    public String toString() {
        return taskId + "-" + operationRef.get();
    }

}

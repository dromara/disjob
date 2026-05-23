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

package cn.ponfee.disjob.worker.base;

import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.util.UuidUtils;
import cn.ponfee.disjob.core.enums.*;
import cn.ponfee.disjob.core.supervisor.dto.StartTaskParam;
import cn.ponfee.disjob.core.supervisor.dto.StartTaskResult;
import cn.ponfee.disjob.core.supervisor.dto.StopTaskParam;
import cn.ponfee.disjob.core.worker.Worker;
import cn.ponfee.disjob.core.worker.dto.ExecuteTaskParam;
import cn.ponfee.disjob.worker.executor.ExecutionResult;
import cn.ponfee.disjob.worker.executor.ExecutionTask;
import cn.ponfee.disjob.worker.executor.JobExecutor;
import cn.ponfee.disjob.worker.executor.Savepoint;
import cn.ponfee.disjob.worker.util.JobExecutorUtils;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.util.Assert;

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

    private final AtomicReference<Operation> operation;
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
    private final String executor;
    private final Worker worker;

    @Getter(AccessLevel.NONE)
    private volatile JobExecutor jobExecutor;

    WorkerTask(ExecuteTaskParam param) {
        this.operation = new AtomicReference<>(Objects.requireNonNull(param.getOperation()));
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
        this.executor = param.getJobExecutor();
        this.worker = Objects.requireNonNull(param.getWorker());
    }

    // --------------------------------------------------------other methods

    Long getLockInstanceId() {
        return wnstanceId != null ? wnstanceId : instanceId;
    }

    Operation getOperation() {
        return operation.get();
    }

    boolean updateOperation(Operation expect, Operation update) {
        return !Objects.equals(expect, update) && operation.compareAndSet(expect, update);
    }

    synchronized ExecutionTask init(StartTaskResult startTaskResult) throws Exception {
        Assert.isNull(jobExecutor, "Job executor already Initialized.");
        Assert.notNull(startTaskResult, "Start task result cannot be null.");
        this.jobExecutor = JobExecutorUtils.loadJobExecutor(executor);
        ExecutionTask executionTask = buildExecutionTask(startTaskResult);
        jobExecutor.init(executionTask);
        return executionTask;
    }

    ExecutionResult execute(ExecutionTask executionTask, Savepoint savepoint) throws Exception {
        return jobExecutor.execute(executionTask, savepoint);
    }

    void destroy() {
        final JobExecutor executor = jobExecutor;
        if (executor != null) {
            ThrowingRunnable.doCaught(executor::destroy, () -> "Destroy task executor error: " + taskId);
        }
    }

    void stop() {
        final JobExecutor executor = jobExecutor;
        if (executor != null) {
            executor.stop();
        }
    }

    StartTaskParam toStartTaskParam() {
        return StartTaskParam.of(jobId, wnstanceId, instanceId, taskId, jobType, worker.serialize(), UuidUtils.uuid32());
    }

    StopTaskParam toStopTaskParam(Operation ops, ExecuteStatus toStatus, String errorMsg) {
        return StopTaskParam.of(wnstanceId, instanceId, taskId, worker.serialize(), ops, toStatus, errorMsg);
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
            && this.operation.get() == that.operation.get();
    }

    @Override
    public int hashCode() {
        return Long.hashCode(taskId) + operation.get().ordinal() * 31;
    }

    @Override
    public String toString() {
        return taskId + "-" + operation.get();
    }

    private ExecutionTask buildExecutionTask(StartTaskResult source) {
        ExecutionTask target = new ExecutionTask();
        target.setTaskId(taskId);
        target.setJobId(jobId);
        target.setRetryCount(retryCount);
        target.setRetriedCount(retriedCount);
        target.setBroadcast(routeStrategy.isBroadcast());
        target.setJobType(jobType);
        target.setWnstanceId(wnstanceId);
        target.setInstanceId(instanceId);
        target.setTriggerTime(new Date(triggerTime));

        target.setTaskNo(source.getTaskNo());
        target.setTaskCount(source.getTaskCount());
        target.setExecuteSnapshot(source.getExecuteSnapshot());
        target.setTaskParam(source.getTaskParam());

        return target;
    }

}

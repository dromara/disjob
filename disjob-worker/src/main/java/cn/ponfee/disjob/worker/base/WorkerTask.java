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

import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.dto.supervisor.StartTaskParam;
import cn.ponfee.disjob.core.dto.supervisor.StopTaskParam;
import cn.ponfee.disjob.core.enums.*;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import cn.ponfee.disjob.worker.handle.TaskExecutor;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Worker task
 *
 * @author Ponfee
 */
@Getter
class WorkerTask {

    /**
     * 任务的操作类型
     */
    @Getter(AccessLevel.NONE)
    private final AtomicReference<Operation> operation;

    private final long taskId;
    private final long instanceId;
    private final Long wnstanceId;
    private final long triggerTime;
    private final long jobId;
    private final JobType jobType;
    private final RouteStrategy routeStrategy;
    private final RedeployStrategy redeployStrategy;
    private final int executeTimeout;
    private final String jobHandler;
    private final Worker worker;

    /**
     * 任务执行处理器
     */
    @Getter(AccessLevel.NONE)
    private final AtomicReference<TaskExecutor> taskExecutor = new AtomicReference<>();

    WorkerTask(ExecuteTaskParam param) {
        this.operation = new AtomicReference<>(param.getOperation());
        this.taskId = param.getTaskId();
        this.instanceId = param.getInstanceId();
        this.wnstanceId = param.getWnstanceId();
        this.triggerTime = param.getTriggerTime();
        this.jobId = param.getJobId();
        this.jobType = param.getJobType();
        this.routeStrategy = param.getRouteStrategy();
        this.redeployStrategy = param.getRedeployStrategy();
        this.executeTimeout = param.getExecuteTimeout();
        this.jobHandler = param.getJobHandler();
        this.worker = param.getWorker();
    }

    // --------------------------------------------------------other methods

    StartTaskParam toStartTaskParam() {
        return new StartTaskParam(wnstanceId, instanceId, taskId, jobType, worker);
    }

    StopTaskParam toStopTaskParam(Operation ops, ExecuteState toState, String errorMsg) {
        return new StopTaskParam(wnstanceId, instanceId, taskId, worker.serialize(), ops, toState, errorMsg);
    }

    Long getLockedKey() {
        return wnstanceId != null ? wnstanceId : instanceId;
    }

    boolean updateOperation(Operation expect, Operation update) {
        return !Objects.equals(expect, update)
            && operation.compareAndSet(expect, update);
    }

    Operation getOperation() {
        return operation.get();
    }

    void bindTaskExecutor(TaskExecutor executor) {
        taskExecutor.set(executor);
    }

    void stop() {
        TaskExecutor executor = taskExecutor.get();
        if (executor != null) {
            executor.stop();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WorkerTask that = (WorkerTask) o;
        return this.taskId == that.taskId
            && this.operation.get() == that.operation.get();
    }

    @Override
    public int hashCode() {
        return Long.hashCode(taskId) + operation.get().ordinal() * 31;
    }

    @Override
    public String toString() {
        return taskId + "-" + operation;
    }

}

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

package cn.ponfee.disjob.supervisor.base;

import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.Operation;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.core.enums.ShutdownStrategy;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import cn.ponfee.disjob.supervisor.model.SchedInstance;
import cn.ponfee.disjob.supervisor.model.SchedJob;
import org.springframework.util.Assert;

import java.util.Objects;

/**
 * ExecuteTaskParam builder
 *
 * @author Ponfee
 */
public final class ExecuteTaskParamBuilder {

    private final SchedJob job;
    private final SchedInstance instance;

    public ExecuteTaskParamBuilder(SchedJob job, SchedInstance instance) {
        if (!instance.getJobId().equals(job.getJobId())) {
            throw new IllegalArgumentException("Inconsistent job id: " + instance.getJobId() + "!=" + job.getJobId());
        }
        this.job = job;
        this.instance = instance;
    }

    public ExecuteTaskParam build(Operation operation, long taskId, long triggerTime, Worker worker) {
        ExecuteTaskParam param = new ExecuteTaskParam();
        param.setOperation(Objects.requireNonNull(operation, "Operation cannot be null."));
        param.setTaskId(taskId);
        param.setInstanceId(instance.getInstanceId());
        param.setWnstanceId(instance.getWnstanceId());
        param.setTriggerTime(triggerTime);
        param.setJobId(job.getJobId());
        param.setRetryCount(job.getRetryCount());
        param.setRetriedCount(instance.getRetriedCount());
        param.setJobType(JobType.of(job.getJobType()));
        param.setRouteStrategy(RouteStrategy.of(job.getRouteStrategy()));
        param.setShutdownStrategy(ShutdownStrategy.of(job.getShutdownStrategy()));
        param.setExecuteTimeout(job.getExecuteTimeout());
        param.setSupervisorAuthenticationToken(Supervisor.local().createSupervisorAuthenticationToken(job.getGroup()));
        param.setWorker(worker);
        param.setJobExecutor(obtainJobExecutor());
        return param;
    }

    private String obtainJobExecutor() {
        if (!instance.isWorkflow()) {
            Assert.hasText(job.getJobExecutor(), () -> "General job executor cannot be null: " + job.getJobId());
            return job.getJobExecutor();
        }

        String curJobExecutor = instance.parseWorkflowCurNode().getName();
        Assert.hasText(curJobExecutor, () -> "Curr node job executor cannot be empty: " + instance.getInstanceId());
        return curJobExecutor;
    }

}

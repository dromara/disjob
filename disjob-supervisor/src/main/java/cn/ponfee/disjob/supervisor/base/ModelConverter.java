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

import cn.ponfee.disjob.alert.base.AlertInstanceEvent;
import cn.ponfee.disjob.alert.base.AlertType;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.core.dag.PredecessorInstance;
import cn.ponfee.disjob.core.dag.PredecessorTask;
import cn.ponfee.disjob.core.dto.supervisor.StartTaskResult;
import cn.ponfee.disjob.core.dto.worker.SplitJobParam;
import cn.ponfee.disjob.core.dto.worker.VerifyJobParam;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.core.enums.RunState;
import cn.ponfee.disjob.core.enums.RunType;
import cn.ponfee.disjob.supervisor.model.SchedInstance;
import cn.ponfee.disjob.supervisor.model.SchedJob;
import cn.ponfee.disjob.supervisor.model.SchedTask;
import cn.ponfee.disjob.supervisor.model.SchedWorkflow;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.List;

/**
 * Model converter
 *
 * @author Ponfee
 */
public final class ModelConverter {

    public static PredecessorInstance toPredecessorInstance(SchedWorkflow workflow, List<SchedTask> tasks) {
        PredecessorInstance instance = new PredecessorInstance();
        instance.setInstanceId(workflow.getInstanceId());
        instance.setCurNode(workflow.getCurNode());
        instance.setTasks(Collects.convert(tasks, ModelConverter::toPredecessorTask));
        return instance;
    }

    public static VerifyJobParam toVerifyJobParam(SchedJob job) {
        VerifyJobParam param = new VerifyJobParam();
        param.fillSupervisorAuthenticationToken(job.getGroup());
        param.setJobExecutor(job.getJobExecutor());
        param.setJobParam(job.getJobParam());
        param.setJobType(JobType.of(job.getJobType()));
        param.setRouteStrategy(RouteStrategy.of(job.getRouteStrategy()));

        param.check();
        return param;
    }

    public static SplitJobParam toSplitJobParam(SchedJob job, SchedInstance instance) {
        Assert.isTrue(JobType.of(job.getJobType()).isGeneral(), "Job must be general.");
        return toSplitJobParam(job, instance.getRetriedCount(), job.getJobExecutor(), null);
    }

    public static SplitJobParam toSplitJobParam(SchedJob job, SchedInstance instance,
                                                List<PredecessorInstance> predecessorInstances) {
        Assert.isTrue(JobType.of(job.getJobType()).isWorkflow(), "Job must be workflow.");
        Assert.isTrue(instance.isWorkflowNode(), () -> "Split job must be node instance: " + instance);
        String curJobExecutor = instance.parseWorkflowCurNode().getName();
        return toSplitJobParam(job, instance.getRetriedCount(), curJobExecutor, predecessorInstances);
    }

    public static StartTaskResult toStartTaskResult(SchedTask task) {
        StartTaskResult result = new StartTaskResult();
        result.setSuccess(true);
        result.setTaskId(task.getTaskId());
        result.setTaskNo(task.getTaskNo());
        result.setTaskCount(task.getTaskCount());
        result.setExecuteSnapshot(task.getExecuteSnapshot());
        result.setTaskParam(task.getTaskParam());
        return result;
    }

    public static AlertInstanceEvent toAlertInstanceEvent(SchedJob job, SchedInstance original, SchedInstance current) {
        AlertType alertType = original.isCompleted() ? AlertType.NOTICE : AlertType.ALARM;
        if (!alertType.matches(job.getAlertOptions())) {
            return null;
        }

        AlertInstanceEvent event = new AlertInstanceEvent();
        event.setGroup(job.getGroup());
        event.setJobName(job.getJobName());
        event.setJobId(job.getJobId());
        event.setAlertType(alertType);
        event.setInstanceId(original.getInstanceId());
        event.setRunType(RunType.of(original.getRunType()));
        event.setRunState(RunState.of(original.getRunState()));
        event.setTriggerTime(new Date(original.getTriggerTime()));
        event.setRunStartTime(original.getRunStartTime());
        event.setRunEndTime(current.getRunEndTime());
        // 如果是DAG任务，original和current都是为lead实例，此时的retriedCount始终为0
        event.setRetriedCount(current.getRetriedCount());
        return event;
    }

    // ----------------------------------------------------------------------private methods

    private static PredecessorTask toPredecessorTask(SchedTask source) {
        PredecessorTask target = new PredecessorTask();
        target.setTaskId(source.getTaskId());
        target.setTaskNo(source.getTaskNo());
        target.setTaskCount(source.getTaskCount());
        target.setExecuteSnapshot(source.getExecuteSnapshot());
        return target;
    }

    private static SplitJobParam toSplitJobParam(SchedJob job, int retriedCount, String jobExecutor,
                                                 List<PredecessorInstance> predecessorInstances) {
        SplitJobParam param = new SplitJobParam();
        param.fillSupervisorAuthenticationToken(job.getGroup());
        param.setJobExecutor(jobExecutor);
        param.setJobParam(job.getJobParam());
        param.setRetryCount(job.getRetryCount());
        param.setRetriedCount(retriedCount);
        param.setJobType(JobType.of(job.getJobType()));
        param.setRouteStrategy(RouteStrategy.of(job.getRouteStrategy()));
        param.setPredecessorInstances(predecessorInstances);

        param.check();
        return param;
    }

}

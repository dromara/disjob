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

import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.core.dag.PredecessorInstance;
import cn.ponfee.disjob.core.dag.PredecessorTask;
import cn.ponfee.disjob.core.dto.supervisor.StartTaskResult;
import cn.ponfee.disjob.core.dto.worker.SplitJobParam;
import cn.ponfee.disjob.core.dto.worker.VerifyJobParam;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.supervisor.model.SchedInstance;
import cn.ponfee.disjob.supervisor.model.SchedJob;
import cn.ponfee.disjob.supervisor.model.SchedTask;
import cn.ponfee.disjob.supervisor.model.SchedWorkflow;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Data converter
 *
 * @author Ponfee
 */
public final class DataConverter {

    public static PredecessorInstance toPredecessorInstance(SchedWorkflow workflow, List<SchedTask> tasks) {
        PredecessorInstance instance = new PredecessorInstance();
        instance.setInstanceId(workflow.getInstanceId());
        instance.setCurNode(workflow.getCurNode());
        instance.setTasks(Collects.convert(tasks, DataConverter::toPredecessorTask));
        return instance;
    }

    public static VerifyJobParam toVerifyJobParam(SchedJob job) {
        VerifyJobParam param = new VerifyJobParam();
        param.setGroup(job.getGroup());
        param.setJobExecutor(job.getJobExecutor());
        param.setJobParam(job.getJobParam());
        param.setJobType(JobType.of(job.getJobType()));
        param.setRouteStrategy(RouteStrategy.of(job.getRouteStrategy()));

        param.check();
        return param;
    }

    public static SplitJobParam toSplitJobParam(SchedJob job, SchedInstance instance) {
        Assert.isTrue(JobType.GENERAL.equalsValue(job.getJobType()), "Job must be general.");
        return toSplitJobParam(job, instance.getRetriedCount(), job.getJobExecutor(), null);
    }

    public static SplitJobParam toSplitJobParam(SchedJob job, SchedInstance instance,
                                                List<PredecessorInstance> predecessorInstances) {
        Assert.isTrue(JobType.WORKFLOW.equalsValue(job.getJobType()), "Job must be workflow.");
        Assert.isTrue(instance.isWorkflowNode(), () -> "Split job must be node instance: " + instance);
        String curNode = instance.parseAttach().parseCurNode().getName();
        return toSplitJobParam(job, instance.getRetriedCount(), curNode, predecessorInstances);
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
        param.setGroup(job.getGroup());
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

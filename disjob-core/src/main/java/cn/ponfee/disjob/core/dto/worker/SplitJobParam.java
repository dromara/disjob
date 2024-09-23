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

package cn.ponfee.disjob.core.dto.worker;

import cn.ponfee.disjob.core.dag.PredecessorInstance;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedJob;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Split job parameter.
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SplitJobParam extends AuthenticationParam {
    private static final long serialVersionUID = -216622646271234535L;

    private String group;
    private String jobExecutor;
    private String jobParam;
    private JobType jobType;
    private RouteStrategy routeStrategy;

    /**
     * Worker数量
     */
    private int workerCount;

    /**
     * 工作流(DAG)任务的前驱节点实例列表(非工作流任务时，为null)
     */
    private List<PredecessorInstance> predecessorInstances;

    public void check() {
        Assert.hasText(group, "Group cannot be empty.");
        Assert.hasText(jobExecutor, "Job executor cannot be empty.");
        Assert.notNull(jobType, "Job type cannot be null.");
        Assert.notNull(routeStrategy, "Route strategy cannot be null.");
    }

    public static SplitJobParam of(SchedJob job) {
        Assert.isTrue(JobType.GENERAL.equalsValue(job.getJobType()), "Job must be general.");
        return of(job, job.getJobExecutor(), null);
    }

    public static SplitJobParam of(SchedJob job, SchedInstance instance, List<PredecessorInstance> predecessorInstances) {
        Assert.isTrue(JobType.WORKFLOW.equalsValue(job.getJobType()), "Job must be workflow.");
        Assert.isTrue(instance.isWorkflowNode(), () -> "Split job must be node instance: " + instance.getInstanceId());
        return of(job, instance.parseAttach().parseCurNode().getName(), predecessorInstances);
    }

    private static SplitJobParam of(SchedJob job, String jobExecutor, List<PredecessorInstance> predecessorInstances) {
        SplitJobParam param = new SplitJobParam();
        param.setGroup(job.getGroup());
        param.setJobExecutor(jobExecutor);
        param.setJobParam(job.getJobParam());
        param.setJobType(JobType.of(job.getJobType()));
        param.setRouteStrategy(RouteStrategy.of(job.getRouteStrategy()));
        param.setPredecessorInstances(predecessorInstances);

        param.check();
        return param;
    }

}

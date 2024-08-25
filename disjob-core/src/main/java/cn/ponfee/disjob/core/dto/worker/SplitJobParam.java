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

import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedJob;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Split job parameter.
 *
 * @author Ponfee
 */
@Getter
@Setter
@NoArgsConstructor
public class SplitJobParam extends AuthenticationParam {
    private static final long serialVersionUID = -216622646271234535L;

    private String group;
    private String jobExecutor;
    private String jobParam;
    private JobType jobType;
    private RouteStrategy routeStrategy;

    public SplitJobParam(String group, String jobExecutor, String jobParam,
                         JobType jobType, RouteStrategy routeStrategy) {
        this.group = group;
        this.jobExecutor = jobExecutor;
        this.jobParam = jobParam;
        this.jobType = jobType;
        this.routeStrategy = routeStrategy;
    }

    public static SplitJobParam from(SchedJob job, SchedInstance inst) {
        if (inst.isWorkflowLead()) {
            throw new IllegalArgumentException("Split job cannot workflow lead instance: " + inst.getInstanceId());
        }
        String jobExecutor = inst.isWorkflowNode() ? inst.parseAttach().parseCurNode().getName() : job.getJobExecutor();
        return from(job, jobExecutor);
    }

    public static SplitJobParam from(SchedJob job, String jobExecutor) {
        return new SplitJobParam(
            job.getGroup(),
            jobExecutor,
            job.getJobParam(),
            JobType.of(job.getJobType()),
            RouteStrategy.of(job.getRouteStrategy())
        );
    }

}

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

import cn.ponfee.disjob.core.dag.WorkflowInstance;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.RouteStrategy;
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

    private String jobExecutor;
    private String jobParam;
    private int retryCount;
    private int retriedCount;
    private JobType jobType;
    private RouteStrategy routeStrategy;

    /**
     * Worker数量
     */
    private int workerCount;

    /**
     * 工作流(DAG)任务的前驱节点实例列表(若为`非工作流任务`或`工作流第一批任务节点`时，则为null)
     */
    private List<WorkflowInstance> predecessorInstances;

    public void check() {
        Assert.hasText(jobExecutor, "Job executor cannot be empty.");
        Assert.notNull(jobType, "Job type cannot be null.");
        Assert.notNull(routeStrategy, "Route strategy cannot be null.");
    }

}

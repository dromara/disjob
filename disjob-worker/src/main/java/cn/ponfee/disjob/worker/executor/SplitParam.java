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

package cn.ponfee.disjob.worker.executor;

import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.core.dag.PredecessorInstance;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Split param
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SplitParam extends ToJsonString implements java.io.Serializable {
    private static final long serialVersionUID = 6130197382386756271L;

    /**
     * 是否广播任务
     */
    private boolean broadcast;

    /**
     * Worker数量
     */
    private int workerCount;

    /**
     * Job参数
     */
    private String jobParam;

    /**
     * 工作流(DAG)任务的前驱节点实例列表(非工作流任务时，为null)
     */
    private List<PredecessorInstance> predecessorInstances;

}

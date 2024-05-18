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

package cn.ponfee.disjob.core.model;

import cn.ponfee.disjob.common.base.ToJsonString;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Abstract execution task
 *
 * @author Ponfee
 */
@Getter
@Setter
public abstract class AbstractExecutionTask extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 6002495716472663520L;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 当前任务序号(从1开始)
     */
    private Integer taskNo;

    /**
     * 任务总数量
     */
    private Integer taskCount;

    /**
     * 保存的执行快照数据
     */
    private String executeSnapshot;

}

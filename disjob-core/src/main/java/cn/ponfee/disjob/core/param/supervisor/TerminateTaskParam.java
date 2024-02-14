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

package cn.ponfee.disjob.core.param.supervisor;

import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.core.enums.ExecuteState;
import cn.ponfee.disjob.core.enums.Operation;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Objects;

/**
 * Terminate task parameter.
 *
 * @author Ponfee
 */
@Getter
@Setter
@NoArgsConstructor
public class TerminateTaskParam extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 7700836087189718161L;

    private long instanceId;
    private Long wnstanceId;
    private long taskId;
    private Operation operation;
    private ExecuteState toState;
    private String errorMsg;
    private String worker;

    public TerminateTaskParam(long instanceId, Long wnstanceId, long taskId, String worker,
                              Operation operation, ExecuteState toState, String errorMsg) {
        Assert.hasText(worker, "Terminate task worker param cannot be blank.");
        this.instanceId = instanceId;
        this.wnstanceId = wnstanceId;
        this.taskId = taskId;
        this.worker = worker;
        this.operation = Objects.requireNonNull(operation, "Terminate task operation param cannot be null.");
        this.toState = Objects.requireNonNull(toState, "Terminate task target state param cannot be null.");
        this.errorMsg = errorMsg;
    }

}

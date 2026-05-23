/*
 * Copyright 2022-2026 Ponfee (http://www.ponfee.cn/)
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

package cn.ponfee.disjob.core.supervisor.dto;

import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.core.enums.ExecuteStatus;
import cn.ponfee.disjob.core.enums.Operation;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * Stop task parameter.
 *
 * @author Ponfee
 */
@Getter
@Setter
public class StopTaskParam extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 7700836087189718161L;

    private Long wnstanceId;
    private long instanceId;
    private long taskId;
    private Operation operation;
    private ExecuteStatus toStatus;
    private String errorMsg;
    private String worker;

    public static StopTaskParam of(Long wnstanceId, long instanceId, long taskId, String worker,
                                   Operation operation, ExecuteStatus toStatus, String errorMsg) {
        StopTaskParam param = new StopTaskParam();
        param.setWnstanceId(wnstanceId);
        param.setInstanceId(instanceId);
        param.setTaskId(taskId);
        param.setWorker(worker);
        param.setOperation(operation);
        param.setToStatus(toStatus);
        param.setErrorMsg(errorMsg);

        param.check();
        return param;
    }

    public void check() {
        Assert.hasText(worker, "Stop task worker cannot be blank.");
        Assert.notNull(operation, "Stop task operation cannot be null.");
        Assert.notNull(toStatus, "Stop task target status cannot be null.");
        // MANUAL_CANCEL: EXECUTING -> MANUAL_CANCELED
        // SHUTDOWN_RESUME: EXECUTING -> WAITING
        Assert.isTrue(toStatus != ExecuteStatus.EXECUTING, "Stop task target status cannot be EXECUTING.");
    }

}

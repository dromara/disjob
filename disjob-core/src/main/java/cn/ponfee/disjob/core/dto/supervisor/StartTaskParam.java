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

package cn.ponfee.disjob.core.dto.supervisor;

import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.core.enums.JobType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * Start task parameter.
 *
 * @author Ponfee
 */
@Getter
@Setter
public class StartTaskParam extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 7700836087189718161L;

    private long jobId;
    private Long wnstanceId;
    private long instanceId;
    private long taskId;
    private String worker;
    private String startRequestId;
    private JobType jobType;

    public static StartTaskParam of(long jobId, Long wnstanceId, long instanceId, long taskId,
                                    JobType jobType, String worker, String startRequestId) {
        StartTaskParam param = new StartTaskParam();
        param.setJobId(jobId);
        param.setWnstanceId(wnstanceId);
        param.setInstanceId(instanceId);
        param.setTaskId(taskId);
        param.setJobType(jobType);
        param.setWorker(worker);
        param.setStartRequestId(startRequestId);

        param.check();
        return param;
    }

    public void check() {
        Assert.hasText(worker, "Start task worker cannot be empty.");
        Assert.hasText(startRequestId, "Start task request id cannot be empty.");
        Assert.notNull(jobType, "Start task job type cannot be null.");
    }

}

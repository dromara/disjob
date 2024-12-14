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

package cn.ponfee.disjob.supervisor.application.request;

import cn.ponfee.disjob.common.model.PageRequest;
import cn.ponfee.disjob.supervisor.application.AuthorizeGroupService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

import java.util.Date;

/**
 * Sched instance page request
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedInstancePageRequest extends PageRequest {
    private static final long serialVersionUID = 2550102303488212001L;

    private Long jobId;
    private Long instanceId;
    private Integer runType;
    private Integer runState;
    private Date startTime;
    private Date endTime;
    private boolean root;

    public void authorize(String user, AuthorizeGroupService authorizeGroupService) {
        Assert.isTrue(jobId != null || instanceId != null, "JobId和InstanceId请至少输入一项");
        if (jobId != null) {
            authorizeGroupService.authorizeJob(user, jobId);
        }
        if (instanceId != null) {
            authorizeGroupService.authorizeInstance(user, instanceId);
        }
    }

}

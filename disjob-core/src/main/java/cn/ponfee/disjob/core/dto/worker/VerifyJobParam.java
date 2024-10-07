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
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

/**
 * Verify job parameter.
 *
 * @author Ponfee
 */
@Getter
@Setter
public class VerifyJobParam extends AuthenticationParam {
    private static final long serialVersionUID = -216622646271234535L;

    private String group;
    private String jobExecutor;
    private String jobParam;
    private JobType jobType;
    private RouteStrategy routeStrategy;

    public void check() {
        Assert.hasText(group, "Group cannot be empty.");
        Assert.hasText(jobExecutor, "Job executor cannot be empty.");
        Assert.notNull(jobType, "Job type cannot be null.");
        Assert.notNull(routeStrategy, "Route strategy cannot be null.");
    }

}

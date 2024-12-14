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

import cn.ponfee.disjob.supervisor.application.converter.SchedJobConverter;
import cn.ponfee.disjob.supervisor.model.SchedJob;
import lombok.Getter;
import lombok.Setter;

/**
 * Update sched job request parameter structure.
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedJobUpdateRequest extends SchedJobAddRequest {
    private static final long serialVersionUID = -1481890923435762900L;

    private Long jobId;
    private Integer version;

    @Override
    public SchedJob tosSchedJob(String user) {
        SchedJob job = SchedJobConverter.INSTANCE.convert(this);
        job.setUpdatedBy(user);
        return job;
    }

}

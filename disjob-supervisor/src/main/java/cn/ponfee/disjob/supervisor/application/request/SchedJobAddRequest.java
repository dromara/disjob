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

import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.supervisor.application.converter.SchedJobConverter;
import cn.ponfee.disjob.supervisor.model.SchedJob;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * Add sched job request parameter structure.
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedJobAddRequest extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -3122300447277606053L;

    private String group;
    private String jobName;
    private String jobExecutor;
    private Integer jobState;
    private Integer jobType;
    private String jobParam;
    private Integer retryType;
    private Integer retryCount;
    private Integer retryInterval;
    private Date startTime;
    private Date endTime;
    private Integer triggerType;
    private String triggerValue;
    private Integer executeTimeout;
    private Integer collidedStrategy;
    private Integer misfireStrategy;
    private Integer routeStrategy;
    private Integer shutdownStrategy;
    private Integer alertOptions;
    private String remark;

    public SchedJob tosSchedJob(String user) {
        SchedJob job = SchedJobConverter.INSTANCE.convert(this);
        job.setCreatedBy(user);
        job.setUpdatedBy(user);
        return job;
    }

}

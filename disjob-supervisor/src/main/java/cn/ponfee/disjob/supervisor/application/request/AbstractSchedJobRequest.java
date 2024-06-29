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
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * Abstract sched job request parameter structure.
 *
 * @author Ponfee
 */
@Getter
@Setter
public abstract class AbstractSchedJobRequest extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -2765296042332021176L;

    private String group;
    private String jobName;
    private String jobHandler;
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
    private Integer redeployStrategy;
    private String remark;

}

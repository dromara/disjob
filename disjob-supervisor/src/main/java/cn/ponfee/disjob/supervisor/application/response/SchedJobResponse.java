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

package cn.ponfee.disjob.supervisor.application.response;

import cn.ponfee.disjob.common.base.ToJsonString;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * Schedule job response structure.
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedJobResponse extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -989147023126011287L;

    /**
     * <pre>
     * 返回给端上浏览器JavaScript Number数值过大时会有问题：Number.MAX_SAFE_INTEGER = 9007199254740991
     * 当数值大于`9007199254740991`时就有可能会丢失精度：1234567891011121314 -> 1234567891011121400
     *
     * 方式一：spring.jackson.generator.write_numbers_as_strings=true
     * 方式二：@JsonSerialize(using = ToStringSerializer.class)
     * 方式三：@JsonFormat(shape = JsonFormat.Shape.STRING)
     * </pre>
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long jobId;

    private String group;
    private String jobName;
    private Integer jobType;
    private String jobExecutor;
    private Integer jobState;
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
    private Long lastTriggerTime;

    /**
     * 因为`FIXED_RATE、FIXED_DELAY`类型会设置为Long.MAX_VALUE，所以这里需要转String解决丢失精度问题
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long nextTriggerTime;

    private String remark;

    private Integer version;
    private Date updatedAt;
    private Date createdAt;
    private String updatedBy;
    private String createdBy;

}

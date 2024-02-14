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
 * Schedule task response structure.
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedTaskResponse extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 3629610339544019607L;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long taskId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long instanceId;

    private Integer taskNo;
    private Integer taskCount;
    private String taskParam;
    private Date executeStartTime;
    private Date executeEndTime;
    private Long executeDuration;
    private Integer executeState;
    private String executeSnapshot;
    private String worker;
    private String errorMsg;

}

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
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.supervisor.application.converter.SchedJobConverter;
import cn.ponfee.disjob.supervisor.model.SchedInstance;
import cn.ponfee.disjob.supervisor.model.SchedTask;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Schedule instance response structure.
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedInstanceResponse extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -6772222626245934369L;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long instanceId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long rnstanceId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long pnstanceId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long wnstanceId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long jobId;

    private Long triggerTime;
    private Integer runType;
    private Integer runState;
    private Boolean retrying;
    private Date runStartTime;
    private Date runEndTime;
    private Long runDuration;
    private Integer retriedCount;
    private String workflowCurNode;

    /**
     * 是否有子节点：0-无；1-有；
     */
    private Integer isTreeLeaf;

    private List<SchedTaskResponse> tasks;

    public static SchedInstanceResponse of(SchedInstance instance, List<SchedTask> tasks) {
        if (instance == null) {
            return null;
        }

        SchedInstanceResponse instanceResponse = SchedJobConverter.INSTANCE.convert(instance);
        instanceResponse.setTasks(Collects.convert(tasks, SchedJobConverter.INSTANCE::convert));
        return instanceResponse;
    }

}

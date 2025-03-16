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

package cn.ponfee.disjob.alert.event;

import cn.ponfee.disjob.core.enums.RunState;
import cn.ponfee.disjob.core.enums.RunType;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Alert instance event
 *
 * @author Ponfee
 */
@Getter
@Setter
public class AlertInstanceEvent extends AlarmEvent {

    private static final long serialVersionUID = 5213948727010283020L;

    /**
     * The group
     */
    private String group;

    /**
     * The job name
     */
    private String jobName;

    /**
     * The job id
     */
    private long jobId;

    /**
     * The instance id
     */
    private long instanceId;

    /**
     * 运行类型
     */
    private RunType runType;

    /**
     * 运行状态
     */
    private RunState runState;

    /**
     * 触发时间
     */
    private Date triggerTime;

    /**
     * 运行开始时间
     */
    private Date runStartTime;

    /**
     * 运行结束时间
     */
    private Date runEndTime;

    /**
     * 已重试的次数
     */
    private int retriedCount;

}

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

import cn.ponfee.disjob.core.enums.ExecuteState;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Alert task event
 *
 * @author Ponfee
 */
@Getter
@Setter
public class AlertTaskEvent extends AlertEvent {

    private static final long serialVersionUID = 5550051265102992301L;

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
     * The task id
     */
    private long taskId;

    /**
     * 执行状态
     */
    private ExecuteState executeState;

    /**
     * 执行开始时间
     */
    private Date executeStartTime;

    /**
     * 执行结束时间
     */
    private Date executeEndTime;

    /**
     * Worker
     */
    private String worker;

    /**
     * 执行错误信息
     */
    private String errorMsg;

    @Override
    public String buildTitle() {
        return null;
    }

    @Override
    public String buildContent() {
        return null;
    }

}

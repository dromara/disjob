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

package cn.ponfee.disjob.dispatch.event;

import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import lombok.Getter;
import lombok.Setter;

/**
 * Task dispatch failed event
 *
 * @author Ponfee
 */
@Getter
@Setter
public class TaskDispatchFailedEvent {

    private long jobId;
    private long instanceId;
    private long taskId;

    public static TaskDispatchFailedEvent of(ExecuteTaskParam task) {
        TaskDispatchFailedEvent event = new TaskDispatchFailedEvent();
        event.setJobId(task.getJobId());
        event.setInstanceId(task.getInstanceId());
        event.setTaskId(task.getTaskId());
        return event;
    }

}

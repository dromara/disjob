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

import lombok.Getter;

/**
 * Task dispatch failed event
 *
 * @author Ponfee
 */
@Getter
public class TaskDispatchFailedEvent {

    private final long jobId;
    private final long instanceId;
    private final long taskId;

    public TaskDispatchFailedEvent(long jobId, long instanceId, long taskId) {
        this.jobId = jobId;
        this.instanceId = instanceId;
        this.taskId = taskId;
    }

}

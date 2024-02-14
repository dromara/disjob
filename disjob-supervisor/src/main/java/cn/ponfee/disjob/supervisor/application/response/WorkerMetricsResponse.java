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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Worker metrics response
 *
 * @author Ponfee
 */
@Getter
@Setter
@NoArgsConstructor
public class WorkerMetricsResponse extends ServerMetricsResponse {
    private static final long serialVersionUID = -8325148543854446360L;

    private String workerId;

    private Boolean alsoSupervisor;
    private Integer jvmThreadActiveCount;
    private Boolean closed;
    private Long keepAliveTime;
    private Integer maximumPoolSize;
    private Integer currentPoolSize;
    private Integer activePoolSize;
    private Integer idlePoolSize;
    private Long queueTaskCount;
    private Long completedTaskCount;

    public WorkerMetricsResponse(String workerId) {
        this.workerId = workerId;
    }

}

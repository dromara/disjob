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

import cn.ponfee.disjob.core.base.Worker;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

/**
 * Configure one worker request
 *
 * @author Ponfee
 */
@Getter
@Setter
public class ConfigureOneWorkerRequest extends ConfigureAllWorkerRequest {
    private static final long serialVersionUID = 8298987323677820526L;

    private String workerId;
    private String host;
    private int port;

    public Worker toWorker() {
        return new Worker(super.getGroup(), StringUtils.isEmpty(workerId) ? "configure" : workerId, host, port);
    }

}

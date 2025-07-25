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
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Server metrics response
 *
 * @author Ponfee
 */
@Getter
@Setter
public abstract class ServerMetricsResponse extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 2989558365810145061L;

    /**
     * Version
     */
    private String version;

    /**
     * Server host
     */
    private String host;

    /**
     * Server port
     */
    private int port;

    /**
     * 启动时间
     */
    private String startupTime;

    /**
     * Response time milliseconds
     */
    private Long responseTime;

}

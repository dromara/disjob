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

package cn.ponfee.disjob.supervisor.base;

import cn.ponfee.disjob.common.base.ToJsonString;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * Supervisor metrics
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SupervisorMetrics extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -3330041841878987072L;

    /**
     * Disjob版本号
     */
    private String version;

    /**
     * 启动时间
     */
    private Date startupTime;

    /**
     * 最近的订阅事件
     */
    private String lastSubscribedEvent;

    /**
     * 是否也是Worker角色
     */
    private boolean alsoWorker;

}

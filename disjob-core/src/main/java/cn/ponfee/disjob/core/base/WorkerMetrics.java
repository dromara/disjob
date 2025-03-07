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

package cn.ponfee.disjob.core.base;

import cn.ponfee.disjob.common.base.ToJsonString;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * Worker metrics
 *
 * @author Ponfee
 */
@Getter
@Setter
public class WorkerMetrics extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -5848721038892533810L;

    /**
     * Disjob版本号
     */
    private String version;

    /**
     * Worker ID
     */
    private String workerId;

    /**
     * 启动时间
     */
    private Date startupTime;

    /**
     * 是否也是Supervisor角色
     */
    private boolean alsoSupervisor;

    /**
     * JVM活跃线程数
     */
    private int jvmThreadActiveCount;

    /**
     * Worker线程池指标情况
     */
    private ThreadPoolMetrics threadPool;

    /**
     * Worker signature
     */
    private String signature;

    @Getter
    @Setter
    public static class ThreadPoolMetrics extends ToJsonString implements Serializable {
        private static final long serialVersionUID = -7745918336704886916L;

        private boolean closed;

        // ------------------------------------thread

        /**
         * Worker thread keep alive time seconds
         */
        private long keepAliveTime;
        private int maximumPoolSize;
        private int currentPoolSize;
        private int activePoolSize;
        private int idlePoolSize;

        // ------------------------------------task

        private long queueTaskCount;
        private long completedTaskCount;
    }

}

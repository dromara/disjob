/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

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
     * 启动时间
     */
    private Date startupAt;

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

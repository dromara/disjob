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

/**
 * Worker thread pool monitor
 *
 * @author Ponfee
 */
@Getter
@Setter
public class WorkerInfo extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -5848721038892533810L;

    private Boolean isSupervisor;
    private ThreadPoolInfo threadPoolInfo;
    private int jvmThreadActiveCount;

    @Getter
    @Setter
    public static class ThreadPoolInfo extends ToJsonString implements Serializable {
        private static final long serialVersionUID = -7745918336704886916L;

        private boolean closed;

        // ------------------------------------thread

        private int keepAliveTime;
        private int maximumPoolSize;
        private int currentPoolSize;
        private int activePoolSize;
        private int idlePoolSize;

        // ------------------------------------task

        private long queueTaskCount;
        private long completedTaskCount;
    }

}

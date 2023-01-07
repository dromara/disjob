/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.dispatch.redis;

import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.Worker;

/**
 * Dispatch utility.
 *
 * @author Ponfee
 */
final class RedisTaskDispatchingUtils {

    static String buildDispatchTasksKey(Worker worker) {
        return String.format(JobConstants.SCHEDULER_KEY_PREFIX + ".dispatch.tasks.%s.%s", worker.getGroup(), worker.getInstanceId());
    }

}

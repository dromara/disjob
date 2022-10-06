package cn.ponfee.scheduler.dispatch;

import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.Worker;

/**
 * Dispatch utility.
 *
 * @author Ponfee
 */
public class DispatchUtils {

    public static String buildDispatchTasksKey(Worker worker) {
        return String.format(JobConstants.SCHEDULER_KEY_PREFIX + ".dispatch.tasks.%s.%s", worker.getGroup(), worker.getInstanceId());
    }

}

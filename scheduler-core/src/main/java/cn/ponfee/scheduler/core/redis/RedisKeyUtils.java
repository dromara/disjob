package cn.ponfee.scheduler.core.redis;

import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.Worker;

/**
 * Build redis key utility
 *
 * @author Ponfee
 */
public class RedisKeyUtils {

    /**
     * Redis key TTL seconds(30 days).
     */
    public static final long REDIS_KEY_TTL_SECONDS = 30L * 86400;

    /**
     * Renew interval milliseconds of redis key TTL value (5 minutes).
     */
    public static final long REDIS_KEY_TTL_RENEW_INTERVAL_MILLIS = 300L * 1000;

    public static String buildDispatchTasksKey(Worker worker) {
        return String.format(JobConstants.KEY_PREFIX + ".dispatch.%s.%s.tasks", worker.getGroup(), worker.getInstanceId());
    }
}

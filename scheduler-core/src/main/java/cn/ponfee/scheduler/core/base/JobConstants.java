package cn.ponfee.scheduler.core.base;

/**
 * Job constants
 *
 * @author Ponfee
 */
public class JobConstants {

    public static final String KEY_PREFIX = "distributed.scheduler";

    /**
     * Redis key TTL seconds(30 days).
     */
    public static final long REDIS_KEY_TTL_SECONDS = 30L * 86400;

    /**
     * Renew interval milliseconds of redis key TTL value (5 minutes).
     */
    public static final long REDIS_KEY_TTL_RENEW_INTERVAL_MILLIS = 300L * 1000;
}

package cn.ponfee.scheduler.core.base;

/**
 * Job constants
 *
 * @author Ponfee
 */
public class JobConstants {

    /**
     * Spring web server port
     */
    public static final String SPRING_WEB_SERVER_PORT = "server.port";

    /**
     * Scheduler configuration key prefix
     */
    public static final String SCHEDULER_KEY_PREFIX = "distributed.scheduler";

    /**
     * Scheduler worker configuration key prefix.
     */
    public static final String WORKER_KEY_PREFIX = SCHEDULER_KEY_PREFIX + ".worker";

    /**
     * Scheduler supervisor configuration key prefix.
     */
    public static final String SUPERVISOR_KEY_PREFIX = SCHEDULER_KEY_PREFIX + ".supervisor";

    /**
     * Http rest connect timeout config key.
     */
    public static final String HTTP_CONNECT_TIMEOUT_KEY = SCHEDULER_KEY_PREFIX + ".http.connect-timeout";

    /**
     * Http rest read timeout config key.
     */
    public static final String HTTP_READ_TIMEOUT_KEY = SCHEDULER_KEY_PREFIX + ".http.read-timeout";

    /**
     * Http max retry times config key.
     */
    public static final String HTTP_MAX_RETRY_TIMES_KEY = SCHEDULER_KEY_PREFIX + ".http.max-retry-times";

    /**
     * Http serialize and deserialize object mapper spring bean name.
     */
    public static final String HTTP_OBJECT_MAPPER_SPRING_BEAN_NAME = SCHEDULER_KEY_PREFIX + ".http.object-mapper";

    /**
     * Current supervisor spring bean name
     */
    public static final String SPRING_BEAN_NAME_CURRENT_WORKER = "currentSupervisor";

    /**
     * Current worker spring bean name
     */
    public static final String SPRING_BEAN_NAME_CURRENT_SUPERVISOR = "currentWorker";

    /**
     * Redis key TTL seconds(30 days).
     */
    public static final long REDIS_KEY_TTL_SECONDS = 30L * 86400;

    /**
     * Renew interval milliseconds of redis key TTL value (5 minutes).
     */
    public static final long REDIS_KEY_TTL_RENEW_INTERVAL_MILLIS = 300L * 1000;
}

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
     * Http rest configuration key prefix.
     */
    public static final String HTTP_KEY_PREFIX = SCHEDULER_KEY_PREFIX + ".http";

    /**
     * Http serialize and deserialize object mapper spring bean name.
     */
    public static final String SPRING_BEAN_NAME_OBJECT_MAPPER = SCHEDULER_KEY_PREFIX + ".object-mapper";

    /**
     * Worker client spring bean name.
     */
    public static final String SPRING_BEAN_NAME_WORKER_CLIENT = "workerClient";

    /**
     * Current supervisor spring bean name
     */
    public static final String SPRING_BEAN_NAME_CURRENT_WORKER = "currentSupervisor";

    /**
     * Current worker spring bean name
     */
    public static final String SPRING_BEAN_NAME_CURRENT_SUPERVISOR = "currentWorker";

}

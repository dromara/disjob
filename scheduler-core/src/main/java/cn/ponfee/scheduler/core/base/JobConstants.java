/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

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
     * Scheduler server registry key prefix
     */
    public static final String SCHEDULER_REGISTRY_KEY_PREFIX = SCHEDULER_KEY_PREFIX + ".registry";

    /**
     * Scheduler configuration namespace
     */
    public static final String SCHEDULER_NAMESPACE = SCHEDULER_KEY_PREFIX + ".namespace";

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
    public static final String SPRING_BEAN_NAME_CURRENT_SUPERVISOR = "currentSupervisor";

    /**
     * Current worker spring bean name
     */
    public static final String SPRING_BEAN_NAME_CURRENT_WORKER = "currentWorker";

    /**
     * Timing wheel spring bean name
     */
    public static final String SPRING_BEAN_NAME_TIMING_WHEEL = "timingWheel";

}

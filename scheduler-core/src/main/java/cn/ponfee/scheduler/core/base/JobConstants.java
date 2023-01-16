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
     * Spring container bean name prefix.
     */
    public static final String SPRING_BEAN_NAME_PREFIX = SCHEDULER_KEY_PREFIX + ".bean";

    /**
     * Http serialize and deserialize object mapper spring bean name.
     */
    public static final String SPRING_BEAN_NAME_OBJECT_MAPPER = SPRING_BEAN_NAME_PREFIX + ".object-mapper";

    /**
     * Current supervisor spring bean name
     */
    public static final String SPRING_BEAN_NAME_CURRENT_SUPERVISOR = SPRING_BEAN_NAME_PREFIX + ".current-supervisor";

    /**
     * Timing wheel spring bean name
     */
    public static final String SPRING_BEAN_NAME_TIMING_WHEEL = SPRING_BEAN_NAME_PREFIX + ".timing-wheel";

    /**
     * Current worker spring bean name
     */
    public static final String SPRING_BEAN_NAME_CURRENT_WORKER = SPRING_BEAN_NAME_PREFIX + ".current-worker";

}

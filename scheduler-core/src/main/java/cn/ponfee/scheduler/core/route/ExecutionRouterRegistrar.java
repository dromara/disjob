/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.route;

import cn.ponfee.scheduler.core.enums.RouteStrategy;

import java.util.Objects;

/**
 * Execution router registrar, also can use SPI implementation.
 *
 * @author Ponfee
 */
public class ExecutionRouterRegistrar {

    private static final ExecutionRouter[] ROUTE_REGISTRIES = new ExecutionRouter[RouteStrategy.values().length];

    static {
        // register default execution router
        register(new RoundRobinExecutionRouter());
        register(new RandomExecutionRouter());
        register(new SimplyHashExecutionRouter());
        register(new ConsistentHashExecutionRouter());
        register(new LocalPriorityExecutionRouter(new RoundRobinExecutionRouter()));
    }

    public static synchronized void register(ExecutionRouter executionRouter) {
        Objects.requireNonNull(executionRouter, "Execution router cannot be null.");
        ROUTE_REGISTRIES[executionRouter.routeStrategy().ordinal()] = executionRouter;
    }

    public static ExecutionRouter get(RouteStrategy routeStrategy) {
        Objects.requireNonNull(routeStrategy, "Route strategy cannot be null.");
        return ROUTE_REGISTRIES[routeStrategy.ordinal()];
    }

}

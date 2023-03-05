/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.route;

import cn.ponfee.scheduler.core.enums.RouteStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Execution router registry, also can use SPI implementation.
 *
 * @author Ponfee
 */
public class ExecutionRouterRegistry {

    private static final Map<RouteStrategy, ExecutionRouter> ROUTE_STRATEGY_MAPPING = new HashMap<>();

    static {
        // register default execution router
        ROUTE_STRATEGY_MAPPING.put(RouteStrategy.RANDOM, new RandomExecutionRouter());
        ROUTE_STRATEGY_MAPPING.put(RouteStrategy.ROUND_ROBIN, new RoundRobinExecutionRouter());
        ROUTE_STRATEGY_MAPPING.put(RouteStrategy.LOCAL_PRIORITY, new LocalPriorityExecutionRouter(new RoundRobinExecutionRouter()));
        ROUTE_STRATEGY_MAPPING.put(RouteStrategy.SIMPLY_HASH, new SimplyHashExecutionRouter());
        ROUTE_STRATEGY_MAPPING.put(RouteStrategy.CONSISTENT_HASH, new ConsistentHashExecutionRouter());
    }

    public static synchronized void register(ExecutionRouter executionRouter) {
        ROUTE_STRATEGY_MAPPING.put(executionRouter.routeStrategy(), executionRouter);
    }

    public static ExecutionRouter get(RouteStrategy routeStrategy) {
        return ROUTE_STRATEGY_MAPPING.get(Objects.requireNonNull(routeStrategy));
    }

}

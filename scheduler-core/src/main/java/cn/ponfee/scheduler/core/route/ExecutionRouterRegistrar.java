/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.route;

import cn.ponfee.scheduler.core.enums.RouteStrategy;
import org.springframework.util.Assert;

import java.util.Objects;

/**
 * Execution router registrar, also can use SPI implementation.
 *
 * @author Ponfee
 * @see java.util.ServiceLoader
 */
public class ExecutionRouterRegistrar {

    private static final ExecutionRouter[] REGISTERED_ROUTES = new ExecutionRouter[RouteStrategy.values().length];

    static {
        // register built-in execution router
        register0(new RoundRobinExecutionRouter());
        register0(new RandomExecutionRouter());
        register0(new SimpleHashExecutionRouter());
        register0(new ConsistentHashExecutionRouter());
        register0(new LocalPriorityExecutionRouter(new RoundRobinExecutionRouter()));
        register0(BroadcastExecutionRouter.INSTANCE);
    }

    private static synchronized void register0(ExecutionRouter executionRouter) {
        REGISTERED_ROUTES[executionRouter.routeStrategy().ordinal()] = executionRouter;
    }

    public static synchronized void register(ExecutionRouter executionRouter) {
        Assert.isTrue(executionRouter != null, "Register execution router cannot be null.");
        Assert.isTrue(executionRouter.routeStrategy() != null, "Register execution router strategy cannot be null.");
        Assert.isTrue(executionRouter.routeStrategy() != RouteStrategy.BROADCAST, "Cannot register broadcast strategy.");
        register0(executionRouter);
    }

    public static ExecutionRouter get(RouteStrategy routeStrategy) {
        Objects.requireNonNull(routeStrategy, "Route strategy cannot be null.");
        return REGISTERED_ROUTES[routeStrategy.ordinal()];
    }

}

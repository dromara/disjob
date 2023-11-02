/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.dispatch.route;

import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.core.param.ExecuteTaskParam;
import org.springframework.util.Assert;

import java.util.List;
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

        for (int i = 0; i < REGISTERED_ROUTES.length; i++) {
            if (REGISTERED_ROUTES[i] == null) {
                throw new Error("Unset build-in route strategy: " + RouteStrategy.values()[i]);
            }
        }
    }

    private static synchronized void register0(ExecutionRouter executionRouter) {
        REGISTERED_ROUTES[executionRouter.routeStrategy().ordinal()] = executionRouter;
    }

    public static synchronized void register(ExecutionRouter executionRouter) {
        Assert.notNull(executionRouter, "Register execution router cannot be null.");
        Assert.notNull(executionRouter.routeStrategy(), "Register execution router strategy cannot be null.");
        Assert.isTrue(executionRouter.routeStrategy() != RouteStrategy.BROADCAST, "Cannot register broadcast strategy.");
        register0(executionRouter);
    }

    public static void route(RouteStrategy routeStrategy, List<ExecuteTaskParam> tasks, List<Worker> workers) {
        Objects.requireNonNull(routeStrategy, "Route strategy cannot be null.");
        ExecutionRouter router = REGISTERED_ROUTES[routeStrategy.ordinal()];
        router.route(tasks, workers);
    }

}

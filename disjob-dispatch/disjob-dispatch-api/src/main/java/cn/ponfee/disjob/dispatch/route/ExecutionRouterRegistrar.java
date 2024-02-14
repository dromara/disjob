/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.dispatch.route;

import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
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

    private static synchronized void register0(ExecutionRouter executionRouter) {
        REGISTERED_ROUTES[executionRouter.routeStrategy().ordinal()] = executionRouter;
    }
}

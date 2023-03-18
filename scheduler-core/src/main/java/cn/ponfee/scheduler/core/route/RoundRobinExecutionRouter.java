/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.route;

import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.enums.RouteStrategy;
import cn.ponfee.scheduler.core.param.ExecuteTaskParam;
import cn.ponfee.scheduler.core.route.count.AtomicCounter;
import cn.ponfee.scheduler.core.route.count.JdkAtomicCounter;

import java.util.List;

/**
 * RoundRobin algorithm for execution router
 *
 * @author Ponfee
 */
public class RoundRobinExecutionRouter extends ExecutionRouter {

    private final AtomicCounter counter;

    public RoundRobinExecutionRouter() {
        this(new JdkAtomicCounter());
    }

    public RoundRobinExecutionRouter(AtomicCounter counter) {
        this.counter = counter;
    }

    @Override
    public RouteStrategy routeStrategy() {
        return RouteStrategy.ROUND_ROBIN;
    }

    @Override
    protected Worker doRoute(String group, ExecuteTaskParam param, List<Worker> workers) {
        return workers.get((int) (counter.getAndIncrement() % workers.size()));
    }

}

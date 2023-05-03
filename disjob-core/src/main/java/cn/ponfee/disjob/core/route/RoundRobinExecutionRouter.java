/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.route;

import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.core.param.ExecuteTaskParam;
import cn.ponfee.disjob.core.route.count.AtomicCounter;
import cn.ponfee.disjob.core.route.count.JdkAtomicCounter;

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

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
import cn.ponfee.disjob.dispatch.route.count.AtomicCounter;
import cn.ponfee.disjob.dispatch.route.count.JdkAtomicCounter;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * RoundRobin algorithm for execution router
 *
 * @author Ponfee
 */
public class RoundRobinExecutionRouter extends ExecutionRouter {

    private final ConcurrentMap<String, AtomicCounter> groupedCounterMap = new ConcurrentHashMap<>();
    private final Function<String, AtomicCounter> counterFactory;

    public RoundRobinExecutionRouter() {
        this(group -> new JdkAtomicCounter());
    }

    public RoundRobinExecutionRouter(Function<String, AtomicCounter> counterFactory) {
        this.counterFactory = counterFactory;
    }

    @Override
    public RouteStrategy routeStrategy() {
        return RouteStrategy.ROUND_ROBIN;
    }

    @Override
    protected void doRoute(List<ExecuteTaskParam> tasks, List<Worker> workers) {
        String group = workers.get(0).getGroup();
        AtomicCounter counter = groupedCounterMap.computeIfAbsent(group, counterFactory);
        long value = counter.getAndAdd(tasks.size());
        for (ExecuteTaskParam task : tasks) {
            int index = (int) (value++ % workers.size());
            task.setWorker(workers.get(index));
        }
    }

}

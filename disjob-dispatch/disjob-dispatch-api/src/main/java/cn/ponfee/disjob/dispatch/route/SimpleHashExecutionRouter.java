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

import java.util.List;
import java.util.function.ToLongFunction;

/**
 * Simple hash algorithm for execution router
 *
 * @author Ponfee
 */
public class SimpleHashExecutionRouter extends ExecutionRouter {

    private final ToLongFunction<ExecuteTaskParam> hashFunction;

    public SimpleHashExecutionRouter() {
        this(ExecuteTaskParam::getTaskId);
    }

    public SimpleHashExecutionRouter(ToLongFunction<ExecuteTaskParam> hashFunction) {
        this.hashFunction = hashFunction;
    }

    @Override
    public RouteStrategy routeStrategy() {
        return RouteStrategy.SIMPLE_HASH;
    }

    @Override
    protected void doRoute(List<ExecuteTaskParam> tasks, List<Worker> workers) {
        tasks.forEach(task -> {
            int index = (int) (hashFunction.applyAsLong(task) % workers.size());
            task.setWorker(workers.get(index));
        });
    }

}

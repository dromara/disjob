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
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Random algorithm for execution router
 *
 * @author Ponfee
 */
public class RandomExecutionRouter extends ExecutionRouter {

    private final Random random;

    public RandomExecutionRouter() {
        this(new Random());
    }

    public RandomExecutionRouter(Random random) {
        this.random = random;
    }

    @Override
    public RouteStrategy routeStrategy() {
        return RouteStrategy.RANDOM;
    }

    @Override
    protected void doRoute(List<ExecuteTaskParam> tasks, List<Worker> workers) {
        tasks.forEach(task -> {
            Random rd = (random != null) ? random : ThreadLocalRandom.current();
            int index = rd.nextInt(workers.size());
            task.setWorker(workers.get(index));
        });
    }

}

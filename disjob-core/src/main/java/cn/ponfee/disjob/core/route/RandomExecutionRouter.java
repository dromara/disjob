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

import java.util.List;
import java.util.Random;

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
    protected Worker doRoute(String group, ExecuteTaskParam param, List<Worker> workers) {
        return workers.get(random.nextInt(workers.size()));
    }

}

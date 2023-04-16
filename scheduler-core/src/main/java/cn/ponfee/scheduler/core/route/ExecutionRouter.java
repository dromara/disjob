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

import java.util.List;

/**
 * Execution router
 *
 * @author Ponfee
 */
public abstract class ExecutionRouter {

    /**
     * Bind a route strategy
     *
     * @return RouteStrategy
     */
    public abstract RouteStrategy routeStrategy();

    /**
     * Routes one worker
     *
     * @param group   the task job group
     * @param param   the task execution param
     * @param workers the list of worker
     * @return routed worker
     */
    public final Worker route(String group, ExecuteTaskParam param, List<Worker> workers) {
        if (workers == null || workers.isEmpty()) {
            return null;
        }
        return doRoute(group, param, workers);
    }

    /**
     * Routes one worker
     *
     * @param group   the task job group
     * @param param   the task execution param
     * @param workers the list of worker
     * @return routed worker
     */
    protected abstract Worker doRoute(String group, ExecuteTaskParam param, List<Worker> workers);
}

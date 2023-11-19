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
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import org.apache.commons.collections4.CollectionUtils;

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
     * Routes worker for task
     *
     * @param tasks   the task list
     * @param workers the worker list
     */
    public final void route(List<ExecuteTaskParam> tasks, List<Worker> workers) {
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }
        if (CollectionUtils.isEmpty(workers)) {
            throw new ExecutionRouterException("Execution route worker list cannot be empty.");
        }
        doRoute(tasks, workers);
    }

    /**
     * Routes worker for task
     *
     * @param tasks   the task list
     * @param workers the worker list
     */
    protected abstract void doRoute(List<ExecuteTaskParam> tasks, List<Worker> workers);

}

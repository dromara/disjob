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
import java.util.Objects;

/**
 * Local priority execution router.
 *
 * @author Ponfee
 */
public class LocalPriorityExecutionRouter extends ExecutionRouter {

    private final ExecutionRouter outsiderRouter;

    public LocalPriorityExecutionRouter(ExecutionRouter outsiderRouter) {
        this.outsiderRouter = Objects.requireNonNull(outsiderRouter, "Outsider router cannot be null.");
    }

    @Override
    public RouteStrategy routeStrategy() {
        return RouteStrategy.LOCAL_PRIORITY;
    }

    @Override
    protected void doRoute(List<ExecuteTaskParam> tasks, List<Worker> workers) {
        // 查找workers列表中是否有当前的jvm worker
        Worker worker = find(workers, Worker.current());
        if (worker != null) {
            tasks.forEach(task -> task.setWorker(worker));
        } else {
            outsiderRouter.route(tasks, workers);
        }
    }

    private static Worker find(List<Worker> workers, Worker current) {
        if (current == null) {
            return null;
        }
        return workers.stream().filter(current::equals).findAny().orElse(null);
    }

}

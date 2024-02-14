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
        Worker worker = findLocal(workers, Worker.current());
        if (worker != null) {
            tasks.forEach(task -> task.setWorker(worker));
        } else {
            outsiderRouter.route(tasks, workers);
        }
    }

    private static Worker findLocal(List<Worker> workers, Worker current) {
        if (current == null) {
            return null;
        }
        return workers.stream().filter(current::equals).findAny().orElse(null);
    }

}

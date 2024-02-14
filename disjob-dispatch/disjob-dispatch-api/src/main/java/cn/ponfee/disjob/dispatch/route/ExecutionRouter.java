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

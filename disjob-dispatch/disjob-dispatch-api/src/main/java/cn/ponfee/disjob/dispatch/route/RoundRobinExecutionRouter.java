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

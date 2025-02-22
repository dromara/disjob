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
        this(null);
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
        for (ExecuteTaskParam task : tasks) {
            Random rd = (random != null) ? random : ThreadLocalRandom.current();
            int index = rd.nextInt(workers.size());
            task.setWorker(workers.get(index));
        }
    }

}

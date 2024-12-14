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

import cn.ponfee.disjob.common.base.ConsistentHash;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Consistent hash algorithm for execution router.
 *
 * <p><a href="https://www.jianshu.com/p/fadbff6d222e">replicas</a>
 *
 * @author Ponfee
 */
public class ConsistentHashExecutionRouter extends ExecutionRouter {

    private final ConcurrentMap<String, Pair<List<Worker>, ConsistentHash<Worker>>> cache = new ConcurrentHashMap<>();

    private final int virtualCount;
    private final ConsistentHash.HashFunction hashFunction;

    public ConsistentHashExecutionRouter() {
        this(17, ConsistentHash.HashFunction.MURMUR3_32);
    }

    public ConsistentHashExecutionRouter(int virtualCount, ConsistentHash.HashFunction hashFunction) {
        this.virtualCount = virtualCount;
        this.hashFunction = hashFunction;
    }

    @Override
    public RouteStrategy routeStrategy() {
        return RouteStrategy.CONSISTENT_HASH;
    }

    @Override
    protected void doRoute(List<ExecuteTaskParam> tasks, List<Worker> workers) {
        ConsistentHash<Worker> consistentHashRouter = getConsistentHash(workers);
        for (ExecuteTaskParam task : tasks) {
            String key = Long.toString(task.getTaskId());
            task.setWorker(consistentHashRouter.routeNode(key));
        }
    }

    // ------------------------------------------------------private methods

    private ConsistentHash<Worker> getConsistentHash(List<Worker> workers) {
        String group = workers.get(0).getGroup();
        Pair<List<Worker>, ConsistentHash<Worker>> pair = cache.get(group);
        if (pair != null && pair.getLeft() == workers) {
            return pair.getRight();
        }

        synchronized (group.intern()) {
            if ((pair = cache.get(group)) == null) {
                pair = Pair.of(workers, new ConsistentHash<>(workers, virtualCount, Worker::serialize, hashFunction));
                cache.put(group, pair);
            } else if (pair.getLeft() != workers) {
                ConsistentHash<Worker> router = pair.getRight();
                List<Worker> oldWorkers = pair.getLeft();
                List<Worker> newWorkers = workers;
                oldWorkers.stream().filter(e -> !newWorkers.contains(e)).forEach(router::removeNode);
                newWorkers.stream().filter(e -> !oldWorkers.contains(e)).forEach(e -> router.addNode(e, virtualCount));
                pair = Pair.of(newWorkers, router);
                cache.put(group, pair);
            }
            return pair.getRight();
        }
    }

}

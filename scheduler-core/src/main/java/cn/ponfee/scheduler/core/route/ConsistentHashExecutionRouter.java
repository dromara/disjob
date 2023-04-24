/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.route;

import cn.ponfee.scheduler.common.base.ConsistentHash;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.enums.RouteStrategy;
import cn.ponfee.scheduler.core.param.ExecuteTaskParam;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Consistent hash algorithm for execution router
 *
 * @author Ponfee
 */
public class ConsistentHashExecutionRouter extends ExecutionRouter {

    private final Map<String, Pair<List<Worker>, ConsistentHash<Worker>>> cache = new HashMap<>();

    private final int virtualCount;
    private final ConsistentHash.HashFunction hashFunction;

    public ConsistentHashExecutionRouter() {
        this(47, ConsistentHash.HashFunction.FNV);
    }

    public ConsistentHashExecutionRouter(int virtualCount,
                                         ConsistentHash.HashFunction hashFunction) {
        this.virtualCount = virtualCount;
        this.hashFunction = hashFunction;
    }

    @Override
    public RouteStrategy routeStrategy() {
        return RouteStrategy.CONSISTENT_HASH;
    }

    @Override
    protected Worker doRoute(String group, ExecuteTaskParam param, List<Worker> workers) {
        ConsistentHash<Worker> consistentHashRouter = getConsistentHash(group, workers);
        return consistentHashRouter.routeNode(Long.toString(param.getInstanceId()));
    }

    private ConsistentHash<Worker> getConsistentHash(String group, List<Worker> workers) {
        Pair<List<Worker>, ConsistentHash<Worker>> pair = cache.get(group);
        if (pair != null && pair.getLeft() == workers) {
            return pair.getRight();
        }

        synchronized (this) {
            if ((pair = cache.get(group)) != null && pair.getLeft() == workers) {
                return pair.getRight();
            }
            int vc = workers.size() == 1 ? 1 : virtualCount;
            ConsistentHash<Worker> router = new ConsistentHash<>(workers, vc, Worker::serialize, hashFunction);
            cache.put(group, Pair.of(workers, router));
            return router;
        }
    }

}

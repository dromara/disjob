package cn.ponfee.scheduler.core.route;

import cn.ponfee.scheduler.common.util.ConsistentHash;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.param.ExecuteParam;

import java.util.List;

/**
 * Consistent hash algorithm for execution router
 *
 * @author Ponfee
 */
public class ConsistentHashExecutionRouter extends ExecutionRouter {

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
    protected Worker doRoute(ExecuteParam param, List<Worker> workers) {
        ConsistentHash<Worker> consistentHashRouter = new ConsistentHash<>(
            workers, virtualCount, Worker::toString, hashFunction
        );
        return consistentHashRouter.routeNode(Long.toString(param.getJobId()));
    }

}

package cn.ponfee.scheduler.core.enums;

import cn.ponfee.scheduler.common.util.Enums;
import cn.ponfee.scheduler.common.base.IntValue;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.core.route.*;

import java.util.List;
import java.util.Map;

/**
 * The route strategy enum definition.
 * <p>mapped by sched_job.route_strategy
 *
 * @author Ponfee
 */
public enum RouteStrategy implements IntValue<RouteStrategy> {

    /**
     * 轮询
     */
    ROUND_ROBIN(1, new RoundRobinExecutionRouter()),

    /**
     * 随机
     */
    RANDOM(2, new RandomExecutionRouter()),

    /**
     * 简单的哈希
     */
    SIMPLY_HASH(3, new SimplyHashExecutionRouter()),

    /**
     * 一致性哈希
     */
    CONSISTENT_HASH(4, new ConsistentHashExecutionRouter()),

    /**
     * 本地优先(当supervisor同时也是worker角色时生效)
     */
    LOCAL_PRIORITY(5, new LocalPriorityExecutionRouter()),
    ;

    private static final Map<Integer, RouteStrategy> MAPPING = Enums.toMap(RouteStrategy.class, RouteStrategy::value);

    private final int value;
    private final ExecutionRouter router;

    RouteStrategy(int value, ExecutionRouter router) {
        this.value = value;
        this.router = router;
    }

    @Override
    public int value() {
        return value;
    }

    public Worker route(ExecuteParam param, List<Worker> workers) {
        return router.route(param, workers);
    }

    public static RouteStrategy of(Integer value) {
        if (value == null) {
            throw new IllegalArgumentException("Route strategy cannot be null.");
        }
        RouteStrategy routeStrategy = MAPPING.get(value);
        if (routeStrategy == null) {
            throw new IllegalArgumentException("Invalid route strategy: " + value);
        }
        return routeStrategy;
    }

}

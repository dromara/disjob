package cn.ponfee.scheduler.core.route;

import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.core.route.count.AtomicCounter;
import cn.ponfee.scheduler.core.route.count.JdkAtomicCounter;

import java.util.List;

/**
 * RoundRobin algorithm for execution router
 *
 * @author Ponfee
 */
public class RoundRobinExecutionRouter extends ExecutionRouter {

    private final AtomicCounter counter;

    public RoundRobinExecutionRouter() {
        this(new JdkAtomicCounter());
    }

    public RoundRobinExecutionRouter(AtomicCounter counter) {
        this.counter = counter;
    }

    @Override
    protected Worker doRoute(ExecuteParam param, List<Worker> workers) {
        return workers.get((int) (counter.getAndIncrement() % workers.size()));
    }

}

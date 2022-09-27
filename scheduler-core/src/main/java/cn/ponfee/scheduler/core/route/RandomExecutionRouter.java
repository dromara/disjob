package cn.ponfee.scheduler.core.route;

import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.param.ExecuteParam;

import java.util.List;
import java.util.Random;

/**
 * Random algorithm for execution router
 *
 * @author Ponfee
 */
public class RandomExecutionRouter extends ExecutionRouter {

    private final Random random;

    public RandomExecutionRouter() {
        this(new Random());
    }

    public RandomExecutionRouter(Random random) {
        this.random = random;
    }

    @Override
    protected Worker doRoute(ExecuteParam param, List<Worker> workers) {
        return workers.get(random.nextInt(workers.size()));
    }

}

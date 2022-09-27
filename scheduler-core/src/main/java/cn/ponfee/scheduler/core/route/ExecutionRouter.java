package cn.ponfee.scheduler.core.route;

import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.param.ExecuteParam;

import java.util.List;

/**
 * Execution router
 *
 * @author Ponfee
 */
public abstract class ExecutionRouter {

    /**
     * Routes one worker
     *
     * @param param   the task execution param
     * @param workers the list of worker
     * @return routed worker
     */
    public final Worker route(ExecuteParam param, List<Worker> workers) {
        if (workers == null || workers.isEmpty()) {
            return null;
        }
        return doRoute(param, workers);
    }

    /**
     * Routes one worker
     *
     * @param param   the task execution param
     * @param workers the list of worker
     * @return routed worker
     */
    protected abstract Worker doRoute(ExecuteParam param, List<Worker> workers);
}

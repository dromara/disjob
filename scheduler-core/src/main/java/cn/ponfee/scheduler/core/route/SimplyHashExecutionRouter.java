/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.route;

import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.param.ExecuteParam;

import java.util.List;
import java.util.function.ToLongFunction;

/**
 * Simple hash algorithm for execution router
 *
 * @author Ponfee
 */
public class SimplyHashExecutionRouter extends ExecutionRouter {

    private final ToLongFunction<ExecuteParam> mapper;

    public SimplyHashExecutionRouter() {
        this(param -> Math.abs(param.getJobId()));
    }

    public SimplyHashExecutionRouter(ToLongFunction<ExecuteParam> mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Worker doRoute(ExecuteParam param, List<Worker> workers) {
        return workers.get((int) (mapper.applyAsLong(param) % workers.size()));
    }

}

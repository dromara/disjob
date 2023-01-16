/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.dispatch;

import cn.ponfee.scheduler.core.enums.RouteStrategy;
import cn.ponfee.scheduler.core.param.ExecuteParam;

/**
 * Dispatch param
 *
 * @author Ponfee
 */
class DispatchParam {

    private final ExecuteParam executeParam;
    private final String group;
    private final RouteStrategy routeStrategy;
    private int retried = 0;

    public DispatchParam(ExecuteParam executeParam, String group, RouteStrategy routeStrategy) {
        this.executeParam = executeParam;
        this.group = group;
        this.routeStrategy = routeStrategy;
    }

    public ExecuteParam executeParam() {
        return executeParam;
    }

    public String group() {
        return group;
    }

    public RouteStrategy routeStrategy() {
        return routeStrategy;
    }

    public void retrying() {
        this.retried++;
    }

    public int retried() {
        return retried;
    }

    @Override
    public String toString() {
        return "DispatchParam{" +
            "executeParam=" + executeParam +
            ", group='" + group + '\'' +
            ", routeStrategy=" + routeStrategy +
            ", retried=" + retried +
            '}';
    }
}

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
        return "{" +
            "executeParam=" + executeParam +
            ", group='" + group + '\'' +
            ", routeStrategy=" + routeStrategy +
            ", retried=" + retried +
            '}';
    }
}

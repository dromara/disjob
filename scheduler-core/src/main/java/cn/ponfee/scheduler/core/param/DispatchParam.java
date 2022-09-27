package cn.ponfee.scheduler.core.param;

import cn.ponfee.scheduler.common.base.ToJsonString;
import cn.ponfee.scheduler.core.enums.Operations;
import cn.ponfee.scheduler.core.enums.RouteStrategy;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Task dispatch parameter
 *
 * @author Ponfee
 */
@Getter
@Setter
public class DispatchParam extends ToJsonString implements Serializable {

    private static final long serialVersionUID = -8974323003185034483L;

    private String group;
    private long jobId;
    private RouteStrategy routeStrategy;
    private long triggerTime;
    private long trackId;
    private Operations ops;
    private List<Long> taskIds;

    public DispatchParam() {
    }

    public DispatchParam(String group,
                         long jobId,
                         RouteStrategy routeStrategy,
                         long triggerTime,
                         long trackId,
                         Operations ops,
                         List<Long> taskIds) {
        this.group = group;
        this.jobId = jobId;
        this.routeStrategy = routeStrategy;
        this.triggerTime = triggerTime;
        this.trackId = trackId;
        this.ops = ops;
        this.taskIds = taskIds;
    }

    public List<ExecuteParam> toExecuteParams() {
        return taskIds.stream()
                      .map(taskId -> new ExecuteParam(ops, taskId, trackId, jobId, triggerTime))
                      .collect(Collectors.toList());
    }
}

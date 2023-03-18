/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.param;

import cn.ponfee.scheduler.common.base.ToJsonString;
import cn.ponfee.scheduler.core.enums.ExecuteState;
import cn.ponfee.scheduler.core.enums.Operations;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * Terminate task parameter.
 *
 * @author Ponfee
 */
@Getter
@Setter
public class TerminateTaskParam extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 7700836087189718161L;

    private long instanceId;
    private long taskId;
    private Operations operation;
    private ExecuteState toState;
    private String errorMsg;

    public TerminateTaskParam() {
    }

    public TerminateTaskParam(long instanceId, long taskId, Operations operation,
                              ExecuteState toState, String errorMsg) {
        Assert.notNull(operation, "Terminate task operation param cannot be null.");
        Assert.notNull(toState, "Terminate task target state param cannot be null.");
        this.instanceId = instanceId;
        this.taskId = taskId;
        this.operation = operation;
        this.toState = toState;
        this.errorMsg = errorMsg;
    }

}

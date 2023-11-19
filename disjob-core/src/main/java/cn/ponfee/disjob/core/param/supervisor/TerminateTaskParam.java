/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.param.supervisor;

import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.core.enums.ExecuteState;
import cn.ponfee.disjob.core.enums.Operations;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Objects;

/**
 * Terminate task parameter.
 *
 * @author Ponfee
 */
@Getter
@Setter
@NoArgsConstructor
public class TerminateTaskParam extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 7700836087189718161L;

    private long instanceId;
    private Long wnstanceId;
    private long taskId;
    private Operations operation;
    private ExecuteState toState;
    private String errorMsg;
    private String worker;

    public TerminateTaskParam(long instanceId, Long wnstanceId, long taskId, String worker,
                              Operations operation, ExecuteState toState, String errorMsg) {
        Assert.hasText(worker, "Terminate task worker param cannot be blank.");
        this.instanceId = instanceId;
        this.wnstanceId = wnstanceId;
        this.taskId = taskId;
        this.worker = worker;
        this.operation = Objects.requireNonNull(operation, "Terminate task operation param cannot be null.");
        this.toState = Objects.requireNonNull(toState, "Terminate task target state param cannot be null.");
        this.errorMsg = errorMsg;
    }

}

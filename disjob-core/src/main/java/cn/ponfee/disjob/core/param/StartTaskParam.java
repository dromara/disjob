/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.param;

import cn.ponfee.disjob.common.base.ToJsonString;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * Start task parameter.
 *
 * @author Ponfee
 */
@Getter
@Setter
@NoArgsConstructor
public class StartTaskParam extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 7700836087189718161L;

    private long instanceId;
    private long taskId;
    private String worker;

    public StartTaskParam(long instanceId, long taskId, String worker) {
        Assert.hasText(worker, "Start task worker param cannot be null.");
        this.instanceId = instanceId;
        this.taskId = taskId;
        this.worker = worker;
    }

    public static StartTaskParam from(ExecuteTaskParam param) {
        return new StartTaskParam(param.getInstanceId(), param.getTaskId(), param.getWorker().serialize());
    }

}

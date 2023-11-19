/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.param.supervisor;

import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.core.base.Worker;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Update task worker parameter
 *
 * @author Ponfee
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateTaskWorkerParam extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -6622646278492874535L;

    private long taskId;
    private String worker;

    public UpdateTaskWorkerParam(long taskId, Worker worker) {
        this.taskId = taskId;
        this.worker = worker == null ? null : worker.serialize();
    }

}

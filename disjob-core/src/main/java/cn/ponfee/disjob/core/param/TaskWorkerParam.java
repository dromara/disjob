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

import java.io.Serializable;

/**
 * Task worker parameter
 *
 * @author Ponfee
 */
@Getter
@Setter
@NoArgsConstructor
public class TaskWorkerParam extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -6622646278492874535L;

    private Long taskId;
    private String worker;

    public TaskWorkerParam(Long taskId, String worker) {
        this.taskId = taskId;
        this.worker = worker;
    }

}

/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application.request;

import cn.ponfee.disjob.core.base.Worker;
import lombok.Getter;
import lombok.Setter;

/**
 * Modify one worker config
 *
 * @author Ponfee
 */
@Getter
@Setter
public class ModifyOneWorkerConfigRequest extends ModifyAllWorkerConfigRequest {
    private static final long serialVersionUID = 8298987323677820526L;

    private String workerId;
    private String host;
    private int port;

    public Worker toWorker() {
        return new Worker(super.getGroup(), workerId, host, port);
    }

}

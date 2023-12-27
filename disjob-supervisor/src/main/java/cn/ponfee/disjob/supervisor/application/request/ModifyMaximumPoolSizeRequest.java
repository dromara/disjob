/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application.request;

import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.core.base.Worker;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Modify worker maximum pool size
 *
 * @author Ponfee
 */
@Getter
@Setter
public class ModifyMaximumPoolSizeRequest extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 8298987323677820526L;

    private String group;
    private String workerId;
    private String host;
    private int port;
    private int maximumPoolSize;

    public Worker toWorker() {
        return new Worker(group, workerId, host, port);
    }

}

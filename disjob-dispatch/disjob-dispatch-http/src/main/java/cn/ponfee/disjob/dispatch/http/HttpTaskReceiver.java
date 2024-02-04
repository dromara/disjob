/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.dispatch.http;

import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.common.spring.RpcController;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import cn.ponfee.disjob.dispatch.TaskReceiver;

/**
 * Task receiver based http.
 *
 * @author Ponfee
 */
public class HttpTaskReceiver extends TaskReceiver implements HttpTaskReceiverService, RpcController {

    public HttpTaskReceiver(Worker.Current currentWorker, TimingWheel<ExecuteTaskParam> timingWheel) {
        super(currentWorker, timingWheel);
    }

    @Override
    public boolean doReceive(ExecuteTaskParam param) {
        return super.receive(param);
    }

}

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
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Task receiver based http.
 *
 * @author Ponfee
 */
@Hidden
public class HttpTaskReceiver extends TaskReceiver implements RpcController {

    public HttpTaskReceiver(Worker.Current currentWorker, TimingWheel<ExecuteTaskParam> timingWheel) {
        super(currentWorker, timingWheel);
    }

    @PostMapping(Constants.WORKER_RECEIVE_PATH)
    @Override
    public boolean receive(ExecuteTaskParam param) {
        return super.receive(param);
    }

}

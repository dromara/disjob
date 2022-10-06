package cn.ponfee.scheduler.dispatch.http;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.common.spring.MarkRpcController;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.dispatch.TaskReceiver;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Task receiver based http.
 *
 * @author Ponfee
 */
public class HttpTaskReceiver extends TaskReceiver implements MarkRpcController {

    public HttpTaskReceiver(TimingWheel<ExecuteParam> timingWheel) {
        super(timingWheel);
    }

    @PostMapping(HttpTaskDispatchingConstants.WORKER_RECEIVE_PATH)
    @Override
    public boolean receive(ExecuteParam executeParam) {
        return super.receive(executeParam);
    }

}

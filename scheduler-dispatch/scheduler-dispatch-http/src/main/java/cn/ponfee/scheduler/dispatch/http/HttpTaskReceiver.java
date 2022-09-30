package cn.ponfee.scheduler.dispatch.http;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.common.spring.LocalizedMethodArguments;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.dispatch.TaskReceiver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Task receiver based http.
 *
 * @author Ponfee
 */
@ConditionalOnBean(TimingWheel.class)
@RestController
@LocalizedMethodArguments
public class HttpTaskReceiver extends TaskReceiver {

    public HttpTaskReceiver(TimingWheel<ExecuteParam> timingWheel) {
        super(timingWheel);
    }

    @PostMapping("worker/rpc/task/receive")
    @Override
    public boolean receive(ExecuteParam executeParam) {
        return super.receive(executeParam);
    }

}

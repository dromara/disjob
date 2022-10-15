package cn.ponfee.scheduler.dispatch.http;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.dispatch.TaskDispatcher;
import cn.ponfee.scheduler.registry.DiscoveryRestTemplate;
import org.springframework.web.client.RestTemplate;

/**
 * Dispatch task based http
 *
 * @author Ponfee
 */
public class HttpTaskDispatcher extends TaskDispatcher {

    private final RestTemplate restTemplate;

    public HttpTaskDispatcher(DiscoveryRestTemplate<Worker> discoveryRestTemplate,
                              TimingWheel<ExecuteParam> timingWheel) {
        super(discoveryRestTemplate.getDiscoveryServer(), timingWheel);
        this.restTemplate = discoveryRestTemplate.getRestTemplate();
    }

    @Override
    protected boolean dispatch(ExecuteParam executeParam) throws Exception {
        Worker worker = executeParam.getWorker();
        String url = String.format("http://%s:%d/%s", worker.getHost(), worker.getPort(), Constants.WORKER_RECEIVE_PATH);
        Boolean result = restTemplate.postForEntity(url, new Object[]{executeParam}, Boolean.class).getBody();
        return result != null && result;
    }

}

/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.dispatch.http;

import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import cn.ponfee.disjob.dispatch.TaskDispatcher;
import cn.ponfee.disjob.registry.DiscoveryRestTemplate;
import org.springframework.web.client.RestTemplate;

/**
 * Dispatch task based http
 *
 * @author Ponfee
 */
public class HttpTaskDispatcher extends TaskDispatcher {

    private static final String URL_PATTERN = "http://%s:%d/" + Constants.WORKER_RECEIVE_PATH;

    private final RestTemplate restTemplate;

    public HttpTaskDispatcher(DiscoveryRestTemplate<Worker> discoveryRestTemplate,
                              RetryProperties retryProperties,
                              TimingWheel<ExecuteTaskParam> timingWheel) {
        super(discoveryRestTemplate.getDiscoveryServer(), retryProperties, timingWheel);
        this.restTemplate = discoveryRestTemplate.getRestTemplate();
    }

    @Override
    protected boolean dispatch(ExecuteTaskParam param) {
        Worker worker = param.getWorker();
        String url = String.format(URL_PATTERN, worker.getHost(), worker.getPort());
        Boolean result = restTemplate.postForEntity(url, new Object[]{param}, Boolean.class).getBody();
        return result != null && result;
    }

}

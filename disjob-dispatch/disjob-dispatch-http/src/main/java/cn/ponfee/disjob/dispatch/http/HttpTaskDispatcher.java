/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.dispatch.http;

import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.common.spring.RestTemplateUtils;
import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import cn.ponfee.disjob.dispatch.TaskDispatcher;
import cn.ponfee.disjob.registry.Discovery;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

/**
 * Dispatch task based http
 *
 * @author Ponfee
 */
public class HttpTaskDispatcher extends TaskDispatcher {

    private static final String URL_PATTERN = JobConstants.HTTP_URL_PATTERN + Constants.WORKER_RECEIVE_PATH;

    /**
     * 因为HttpTaskReceiver不是一个接口，所以这里不使用`ServerRestProxy#create`方式
     */
    private final RestTemplate restTemplate;

    public HttpTaskDispatcher(Discovery<Worker> discoveryWorker,
                              RetryProperties retryProperties,
                              TimingWheel<ExecuteTaskParam> timingWheel,
                              RestTemplate restTemplate) {
        super(discoveryWorker, retryProperties, timingWheel);
        this.restTemplate = restTemplate;
    }

    @Override
    protected boolean dispatch(ExecuteTaskParam param) {
        Worker worker = param.getWorker();
        String url = String.format(URL_PATTERN, worker.getHost(), worker.getPort());
        return RestTemplateUtils.invoke(restTemplate, url, HttpMethod.POST, boolean.class, null, param);
    }

}

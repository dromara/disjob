/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.dispatch.http;

import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import cn.ponfee.disjob.dispatch.TaskDispatcher;
import cn.ponfee.disjob.dispatch.TaskReceiver;
import cn.ponfee.disjob.registry.Discovery;
import cn.ponfee.disjob.registry.rpc.DestinationServerRestProxy;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Dispatch task based http
 *
 * @author Ponfee
 */
public class HttpTaskDispatcher extends TaskDispatcher {

    private final DestinationServerRestProxy.DestinationServerInvoker<HttpTaskReceiverService, Worker> httpTaskReceiverClient;

    public HttpTaskDispatcher(Discovery<Worker> discoveryWorker,
                              RetryProperties retryProperties,
                              RestTemplate restTemplate,
                              @Nullable TaskReceiver taskReceiver) {
        super(discoveryWorker, retryProperties, taskReceiver);

        Function<Worker, String> workerContextPath = worker -> Supervisor.current().getWorkerContextPath(worker.getGroup());
        RetryProperties retry = RetryProperties.of(0, 0);
        this.httpTaskReceiverClient = DestinationServerRestProxy.create(HttpTaskReceiverService.class, null, null, workerContextPath, restTemplate, retry);
    }

    @Override
    protected boolean doDispatch(ExecuteTaskParam param) {
        return httpTaskReceiverClient.invoke(param.getWorker(), client -> client.doReceive(param));
    }

}

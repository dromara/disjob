/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.dispatch.http;

import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import cn.ponfee.disjob.dispatch.TaskDispatcher;
import cn.ponfee.disjob.registry.Discovery;
import cn.ponfee.disjob.registry.rpc.DestinationServerRestProxy;
import cn.ponfee.disjob.registry.rpc.DestinationServerRestProxy.DestinationServerClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Dispatch task based http
 *
 * @author Ponfee
 */
public class HttpTaskDispatcher extends TaskDispatcher {

    private final DestinationServerClient<HttpTaskController, Worker> httpTaskReceiverClient;

    public HttpTaskDispatcher(ApplicationEventPublisher eventPublisher,
                              Discovery<Worker> discoverWorker,
                              RetryProperties retryProperties,
                              Supervisor.Local localSupervisor,
                              RestTemplate restTemplate,
                              @Nullable HttpTaskReceiver httpTaskReceiver) {
        super(eventPublisher, discoverWorker, retryProperties, httpTaskReceiver);

        Function<Worker, String> workerContextPath = worker -> localSupervisor.getWorkerContextPath(worker.getGroup());
        RetryProperties retry = RetryProperties.none();
        // `TaskDispatcher#dispatch0`内部有处理本地worker的分派逻辑，这里不需要本地的`HttpTaskController`，所以传null
        this.httpTaskReceiverClient = DestinationServerRestProxy.create(
            HttpTaskController.class,
            null, // httpTaskReceiver
            null, // Worker.local()
            workerContextPath,
            restTemplate,
            retry
        );
    }

    @Override
    protected boolean doDispatch(ExecuteTaskParam param) {
        return httpTaskReceiverClient.call(param.getWorker(), client -> client.receive(param));
    }

}

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

package cn.ponfee.disjob.worker.configuration;

import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.SupervisorRpcService;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.dispatch.TaskReceiver;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.worker.WorkerStartup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.Lifecycle;
import org.springframework.context.Phased;
import org.springframework.context.SmartLifecycle;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RestTemplate;

/**
 * Worker lifecycle
 *
 * @author Ponfee
 */
class WorkerLifecycle implements SmartLifecycle {

    private final WorkerStartup workerStartup;

    WorkerLifecycle(Worker.Local localWorker,
                    WorkerProperties workerProperties,
                    RetryProperties retryProperties,
                    WorkerRegistry workerRegistry,
                    TaskReceiver taskReceiver,
                    @Qualifier(JobConstants.SPRING_BEAN_NAME_REST_TEMPLATE) RestTemplate restTemplate,
                    // if the local server also is a supervisor -> cn.ponfee.disjob.supervisor.provider.SupervisorRpcProvider
                    @Nullable SupervisorRpcService supervisorRpcService) {
        this.workerStartup = new WorkerStartup(
            localWorker,
            workerProperties,
            retryProperties,
            workerRegistry,
            taskReceiver,
            restTemplate,
            supervisorRpcService
        );
    }

    @Override
    public void start() {
        workerStartup.start();
    }

    @Override
    public void stop(Runnable callback) {
        workerStartup.stop();
        callback.run();
    }

    @Override
    public void stop() {
        stop(() -> {});
    }

    @Override
    public boolean isRunning() {
        return workerStartup.isRunning();
    }

    /**
     * <pre>
     * 值越小
     *  {@link Lifecycle#start()}方法越先执行
     *  {@link Lifecycle#stop()}方法越后执行
     *
     * A1#start() -> A2#start() -> A2#stop() -> A1#stop()
     * </pre>
     *
     * @return int value of phase
     * @see Phased#getPhase()
     */
    @Override
    public int getPhase() {
        return DEFAULT_PHASE - 1;
    }

}

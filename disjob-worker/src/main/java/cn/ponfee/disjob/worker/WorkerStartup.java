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

package cn.ponfee.disjob.worker;

import cn.ponfee.disjob.common.base.RetryInvocationHandler;
import cn.ponfee.disjob.common.base.Startable;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.spring.RestTemplateUtils;
import cn.ponfee.disjob.common.util.ProxyUtils;
import cn.ponfee.disjob.core.base.HttpProperties;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.SupervisorRpcService;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.dispatch.TaskReceiver;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.registry.rpc.DiscoveryServerRestProxy;
import cn.ponfee.disjob.worker.base.TimingWheelRotator;
import cn.ponfee.disjob.worker.base.WorkerThreadPool;
import cn.ponfee.disjob.worker.configuration.WorkerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationHandler;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Startup worker.
 *
 * @author Ponfee
 */
public class WorkerStartup implements Startable {

    private static final Logger LOG = LoggerFactory.getLogger(WorkerStartup.class);

    private final Worker.Current currentWorker;
    private final WorkerThreadPool workerThreadPool;
    private final TimingWheelRotator timingWheelRotator;
    private final TaskReceiver taskReceiver;
    private final WorkerRegistry workerRegistry;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private WorkerStartup(Worker.Current currentWorker,
                          HttpProperties httpProperties,
                          WorkerProperties workerProperties,
                          RetryProperties retryProperties,
                          WorkerRegistry workerRegistry,
                          TaskReceiver taskReceiver,
                          @Nullable SupervisorRpcService supervisorRpcService,
                          @Nullable ObjectMapper objectMapper) {
        Objects.requireNonNull(currentWorker, "Current worker cannot null.");
        Objects.requireNonNull(workerProperties, "Worker properties cannot be null.").check();
        Objects.requireNonNull(httpProperties, "Http properties cannot be null.").check();
        Objects.requireNonNull(retryProperties, "Retry properties cannot be null.").check();
        Objects.requireNonNull(workerRegistry, "Server registry cannot null.");
        Objects.requireNonNull(taskReceiver, "Task receiver cannot null.");

        SupervisorRpcService supervisorRpcClient = createProxy(
            supervisorRpcService, httpProperties, retryProperties, workerRegistry, objectMapper
        );

        this.currentWorker = currentWorker;
        this.workerThreadPool = new WorkerThreadPool(
            workerProperties.getMaximumPoolSize(),
            workerProperties.getKeepAliveTimeSeconds(),
            supervisorRpcClient
        );
        this.timingWheelRotator = new TimingWheelRotator(
            supervisorRpcClient,
            workerRegistry,
            taskReceiver.getTimingWheel(),
            workerThreadPool,
            workerProperties.getProcessThreadPoolSize()
        );
        this.taskReceiver = taskReceiver;
        this.workerRegistry = workerRegistry;
    }

    @Override
    public void start() {
        if (!started.compareAndSet(false, true)) {
            LOG.warn("Worker startup already started.");
            return;
        }
        workerThreadPool.start();
        timingWheelRotator.start();
        taskReceiver.start();
        workerRegistry.register(currentWorker);
    }

    @Override
    public void stop() {
        if (!started.compareAndSet(true, false)) {
            LOG.warn("Worker startup already stopped.");
            return;
        }
        ThrowingRunnable.doCaught(workerRegistry::close);
        ThrowingRunnable.doCaught(taskReceiver::close);
        ThrowingRunnable.doCaught(timingWheelRotator::close);
        ThrowingRunnable.doCaught(workerThreadPool::close);
    }

    // ----------------------------------------------------------------------------------------builder

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Worker.Current currentWorker;
        private WorkerProperties workerProperties;
        private HttpProperties httpProperties;
        private RetryProperties retryProperties;
        private WorkerRegistry workerRegistry;
        private TaskReceiver taskReceiver;
        private SupervisorRpcService supervisorRpcService;
        private ObjectMapper objectMapper;

        private Builder() {
        }

        public Builder currentWorker(Worker.Current currentWorker) {
            this.currentWorker = currentWorker;
            return this;
        }

        public Builder workerProperties(WorkerProperties workerProperties) {
            this.workerProperties = workerProperties;
            return this;
        }

        public Builder httpProperties(HttpProperties httpProperties) {
            this.httpProperties = httpProperties;
            return this;
        }

        public Builder retryProperties(RetryProperties retryProperties) {
            this.retryProperties = retryProperties;
            return this;
        }

        public Builder workerRegistry(WorkerRegistry workerRegistry) {
            this.workerRegistry = workerRegistry;
            return this;
        }

        public Builder taskReceiver(TaskReceiver taskReceiver) {
            this.taskReceiver = taskReceiver;
            return this;
        }

        public Builder supervisorRpcService(SupervisorRpcService supervisorRpcService) {
            this.supervisorRpcService = supervisorRpcService;
            return this;
        }

        public Builder objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public WorkerStartup build() {
            return new WorkerStartup(
                currentWorker,
                httpProperties,
                workerProperties,
                retryProperties,
                workerRegistry,
                taskReceiver,
                supervisorRpcService,
                objectMapper
            );
        }
    }

    // ----------------------------------------------------------------------------------------private methods

    private static SupervisorRpcService createProxy(SupervisorRpcService local,
                                                    HttpProperties http,
                                                    RetryProperties retry,
                                                    WorkerRegistry discoverySupervisor,
                                                    ObjectMapper objectMapper) {
        if (local != null) {
            // cn.ponfee.disjob.supervisor.provider.rpc.SupervisorRpcProvider
            // 此Worker同时也是Supervisor身份，则是本地调用，并使用动态代理增加重试能力
            InvocationHandler ih = new RetryInvocationHandler(local, retry.getMaxCount(), retry.getBackoffPeriod());
            return ProxyUtils.create(ih, SupervisorRpcService.class);
        } else {
            RestTemplate restTemplate = RestTemplateUtils.create(http.getConnectTimeout(), http.getReadTimeout(), objectMapper);
            return DiscoveryServerRestProxy.create(SupervisorRpcService.class, discoverySupervisor, restTemplate, retry);
        }
    }

}

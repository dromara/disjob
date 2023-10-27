/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.worker;

import cn.ponfee.disjob.common.base.RetryInvocationHandler;
import cn.ponfee.disjob.common.base.Startable;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.util.ProxyUtils;
import cn.ponfee.disjob.core.base.*;
import cn.ponfee.disjob.dispatch.TaskReceiver;
import cn.ponfee.disjob.registry.DiscoveryRestProxy;
import cn.ponfee.disjob.registry.DiscoveryRestTemplate;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.worker.base.TimingWheelRotator;
import cn.ponfee.disjob.worker.base.WorkerThreadPool;
import cn.ponfee.disjob.worker.configuration.WorkerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final Worker currentWorker;
    private final WorkerThreadPool workerThreadPool;
    private final TimingWheelRotator timingWheelRotator;
    private final TaskReceiver taskReceiver;
    private final WorkerRegistry workerRegistry;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private WorkerStartup(Worker currentWorker,
                          WorkerProperties workerProperties,
                          RetryProperties retryProperties,
                          HttpProperties httpProperties,
                          WorkerRegistry workerRegistry,
                          TaskReceiver taskReceiver,
                          @Nullable SupervisorCoreRpcService supervisorCoreRpcService,
                          @Nullable ObjectMapper objectMapper) {
        Objects.requireNonNull(currentWorker, "Current worker cannot null.");
        Objects.requireNonNull(workerProperties, "Worker properties cannot be null.").check();
        Objects.requireNonNull(retryProperties, "Retry properties cannot be null.").check();
        Objects.requireNonNull(httpProperties, "Http properties cannot be null.").check();
        Objects.requireNonNull(workerRegistry, "Server registry cannot null.");
        Objects.requireNonNull(taskReceiver, "Task receiver cannot null.");

        SupervisorCoreRpcService supervisorCoreRpcClient = createProxy(
            supervisorCoreRpcService, retryProperties, httpProperties, workerRegistry, objectMapper
        );

        this.currentWorker = currentWorker;
        this.workerThreadPool = new WorkerThreadPool(
            workerProperties.getMaximumPoolSize(),
            workerProperties.getKeepAliveTimeSeconds(),
            supervisorCoreRpcClient
        );
        this.timingWheelRotator = new TimingWheelRotator(
            supervisorCoreRpcClient,
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
        ThrowingRunnable.execute(workerRegistry::close);
        ThrowingRunnable.execute(taskReceiver::close);
        ThrowingRunnable.execute(timingWheelRotator::close);
        ThrowingRunnable.execute(workerThreadPool::close);
    }

    // ----------------------------------------------------------------------------------------builder

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Worker currentWorker;
        private WorkerProperties workerProperties;
        private RetryProperties retryProperties;
        private HttpProperties httpProperties;
        private WorkerRegistry workerRegistry;
        private TaskReceiver taskReceiver;
        private SupervisorCoreRpcService supervisorCoreRpcService;
        private ObjectMapper objectMapper;

        private Builder() {
        }

        public Builder currentWorker(Worker currentWorker) {
            this.currentWorker = currentWorker;
            return this;
        }

        public Builder workerProperties(WorkerProperties workerProperties) {
            this.workerProperties = workerProperties;
            return this;
        }

        public Builder retryProperties(RetryProperties retryProperties) {
            this.retryProperties = retryProperties;
            return this;
        }

        public Builder httpProperties(HttpProperties httpProperties) {
            this.httpProperties = httpProperties;
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

        public Builder supervisorCoreRpcService(SupervisorCoreRpcService supervisorCoreRpcService) {
            this.supervisorCoreRpcService = supervisorCoreRpcService;
            return this;
        }

        public Builder objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public WorkerStartup build() {
            return new WorkerStartup(
                currentWorker,
                workerProperties,
                retryProperties,
                httpProperties,
                workerRegistry,
                taskReceiver,
                supervisorCoreRpcService,
                objectMapper
            );
        }
    }

    // ----------------------------------------------------------------------------------------private methods

    private static SupervisorCoreRpcService createProxy(SupervisorCoreRpcService local,
                                                        RetryProperties retry,
                                                        HttpProperties http,
                                                        WorkerRegistry workerRegistry,
                                                        ObjectMapper objectMapper) {
        if (local != null) {
            // 此Worker同时也是Supervisor身份，则本地调用：cn.ponfee.disjob.supervisor.provider.SupervisorCoreRpcProvider
            // 使用动态代理增加重试能力
            InvocationHandler ih = new RetryInvocationHandler(local, retry.getMaxCount(), retry.getBackoffPeriod());
            return ProxyUtils.create(SupervisorCoreRpcService.class, ih);
        } else {
            DiscoveryRestTemplate<Supervisor> discoveryRestTemplate = DiscoveryRestTemplate.<Supervisor>builder()
                .httpConnectTimeout(http.getConnectTimeout())
                .httpReadTimeout(http.getReadTimeout())
                .retryMaxCount(retry.getMaxCount())
                .retryBackoffPeriod(retry.getBackoffPeriod())
                .objectMapper(objectMapper)
                .discoveryServer(workerRegistry)
                .build();
            return DiscoveryRestProxy.create(false, SupervisorCoreRpcService.class, discoveryRestTemplate);
        }
    }

}

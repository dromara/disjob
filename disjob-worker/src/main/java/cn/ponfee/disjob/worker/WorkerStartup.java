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
import java.lang.reflect.Proxy;
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
                          @Nullable SupervisorService supervisorService,
                          @Nullable ObjectMapper objectMapper) {
        Objects.requireNonNull(currentWorker, "Current worker cannot null.");
        Objects.requireNonNull(workerProperties, "Worker properties cannot be null.").check();
        Objects.requireNonNull(retryProperties, "Retry properties cannot be null.").check();
        Objects.requireNonNull(httpProperties, "Http properties cannot be null.").check();
        Objects.requireNonNull(workerRegistry, "Server registry cannot null.");
        Objects.requireNonNull(taskReceiver, "Task receiver cannot null.");

        SupervisorService supervisorServiceClient = createProxy(
            supervisorService, retryProperties, httpProperties, workerRegistry, objectMapper
        );

        this.currentWorker = currentWorker;
        this.workerThreadPool = new WorkerThreadPool(
            workerProperties.getMaximumPoolSize(),
            workerProperties.getKeepAliveTimeSeconds(),
            supervisorServiceClient
        );
        this.timingWheelRotator = new TimingWheelRotator(
            currentWorker,
            supervisorServiceClient,
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
        ThrowingRunnable.caught(workerRegistry::close);
        ThrowingRunnable.caught(taskReceiver::close);
        ThrowingRunnable.caught(timingWheelRotator::close);
        ThrowingRunnable.caught(workerThreadPool::close);
    }

    private static SupervisorService createProxy(SupervisorService supervisorService,
                                                 RetryProperties retry,
                                                 HttpProperties http,
                                                 WorkerRegistry workerRegistry,
                                                 ObjectMapper objectMapper) {
        if (supervisorService != null) {
            // cn.ponfee.disjob.supervisor.rpc.SupervisorServiceProvider
            ClassLoader classLoader = supervisorService.getClass().getClassLoader();
            Class<?>[] interfaces = {SupervisorService.class};
            InvocationHandler ih = new RetryInvocationHandler(supervisorService, retry.getMaxCount(), retry.getBackoffPeriod());
            return (SupervisorService) Proxy.newProxyInstance(classLoader, interfaces, ih);
        } else {
            DiscoveryRestTemplate<Supervisor> discoveryRestTemplate = DiscoveryRestTemplate.<Supervisor>builder()
                .httpConnectTimeout(http.getConnectTimeout())
                .httpReadTimeout(http.getReadTimeout())
                .retryMaxCount(retry.getMaxCount())
                .retryBackoffPeriod(retry.getBackoffPeriod())
                .objectMapper(objectMapper)
                .discoveryServer(workerRegistry)
                .build();
            return DiscoveryRestProxy.create(false, SupervisorService.class, discoveryRestTemplate);
        }
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
        private SupervisorService supervisorService;
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

        public Builder supervisorService(SupervisorService supervisorService) {
            this.supervisorService = supervisorService;
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
                supervisorService,
                objectMapper
            );
        }
    }

}

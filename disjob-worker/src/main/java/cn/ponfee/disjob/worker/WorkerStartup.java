/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.worker;

import cn.ponfee.disjob.common.base.Startable;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.SupervisorService;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.dispatch.TaskReceiver;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.worker.base.RotatingTimingWheel;
import cn.ponfee.disjob.worker.base.WorkerThreadPool;
import cn.ponfee.disjob.worker.configuration.WorkerProperties;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Start up worker.
 *
 * @author Ponfee
 */
public class WorkerStartup implements Startable {

    private final WorkerThreadPool workerThreadPool;
    private final Worker currentWorker;
    private final WorkerRegistry workerRegistry;
    private final TaskReceiver taskReceiver;
    private final RotatingTimingWheel rotatingTimingWheel;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private WorkerStartup(Worker currentWorker,
                          RetryProperties retryProperties,
                          WorkerProperties workerProperties,
                          SupervisorService supervisorServiceClient,
                          WorkerRegistry workerRegistry,
                          TaskReceiver taskReceiver) {
        Objects.requireNonNull(currentWorker, "Current worker cannot null.");
        Objects.requireNonNull(retryProperties, "Retry config cannot be null.").check();
        Objects.requireNonNull(workerProperties, "Worker config cannot be null.").check();
        Objects.requireNonNull(supervisorServiceClient, "Supervisor service client cannot null.");
        Objects.requireNonNull(workerRegistry, "Server registry cannot null.");
        Objects.requireNonNull(taskReceiver, "Task receiver cannot null.");

        this.currentWorker = currentWorker;
        this.workerThreadPool = new WorkerThreadPool(
            workerProperties.getMaximumPoolSize(),
            workerProperties.getKeepAliveTimeSeconds(),
            retryProperties,
            supervisorServiceClient
        );
        this.workerRegistry = workerRegistry;
        this.taskReceiver = taskReceiver;
        this.rotatingTimingWheel = new RotatingTimingWheel(
            currentWorker,
            retryProperties,
            supervisorServiceClient,
            workerRegistry,
            taskReceiver.getTimingWheel(),
            workerThreadPool,
            workerProperties.getUpdateTaskWorkerThreadPoolSize()
        );
    }

    @Override
    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        workerThreadPool.start();
        rotatingTimingWheel.start();
        taskReceiver.start();
        workerRegistry.register(currentWorker);
    }

    @Override
    public void stop() {
        ThrowingRunnable.caught(workerRegistry::close);
        ThrowingRunnable.caught(taskReceiver::close);
        ThrowingRunnable.caught(rotatingTimingWheel::close);
        ThrowingRunnable.caught(workerThreadPool::close);
    }

    // ----------------------------------------------------------------------------------------builder

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Worker currentWorker;
        private RetryProperties retryProperties;
        private WorkerProperties workerConfig;
        private SupervisorService supervisorServiceClient;
        private WorkerRegistry workerRegistry;
        private TaskReceiver taskReceiver;

        private Builder() {
        }

        public Builder currentWorker(Worker currentWorker) {
            this.currentWorker = currentWorker;
            return this;
        }

        public Builder retryProperties(RetryProperties retryProperties) {
            this.retryProperties = retryProperties;
            return this;
        }

        public Builder workerConfig(WorkerProperties workerConfig) {
            this.workerConfig = workerConfig;
            return this;
        }

        public Builder supervisorServiceClient(SupervisorService supervisorServiceClient) {
            this.supervisorServiceClient = supervisorServiceClient;
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

        public WorkerStartup build() {
            return new WorkerStartup(
                currentWorker,
                retryProperties,
                workerConfig,
                supervisorServiceClient,
                workerRegistry,
                taskReceiver
            );
        }
    }

}

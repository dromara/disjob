/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.worker;

import cn.ponfee.scheduler.common.base.exception.Throwables;
import cn.ponfee.scheduler.core.base.SupervisorService;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.dispatch.TaskReceiver;
import cn.ponfee.scheduler.registry.WorkerRegistry;
import cn.ponfee.scheduler.worker.base.WorkerThreadPool;
import cn.ponfee.scheduler.worker.thread.RotatingTimingWheelThread;
import org.springframework.util.Assert;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Start up worker.
 *
 * @author Ponfee
 */
public class WorkerStartup implements AutoCloseable {

    private final WorkerThreadPool workerThreadPool;
    private final Worker currentWorker;
    private final WorkerRegistry workerRegistry;
    private final TaskReceiver taskReceiver;
    private final RotatingTimingWheelThread rotatingTimingWheelThread;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private WorkerStartup(Worker currentWorker,
                          int maximumPoolSize,
                          int keepAliveTimeSeconds,
                          SupervisorService SupervisorServiceClient,
                          WorkerRegistry workerRegistry,
                          TaskReceiver taskReceiver) {
        Assert.notNull(currentWorker, "Current worker cannot null.");
        Assert.isTrue(maximumPoolSize > 0, "Maximum pool size must be greater zero.");
        Assert.isTrue(keepAliveTimeSeconds > 0, "Keep alive time seconds must be greater zero.");
        Assert.notNull(SupervisorServiceClient, "Supervisor service client cannot null.");
        Assert.notNull(workerRegistry, "Server registry cannot null.");
        Assert.notNull(taskReceiver, "Task receiver cannot null.");

        this.currentWorker = currentWorker;
        this.workerThreadPool = new WorkerThreadPool(maximumPoolSize, keepAliveTimeSeconds, SupervisorServiceClient);
        this.workerRegistry = workerRegistry;
        this.taskReceiver = taskReceiver;
        this.rotatingTimingWheelThread = new RotatingTimingWheelThread(
            currentWorker, SupervisorServiceClient, workerRegistry,
            taskReceiver.getTimingWheel(), workerThreadPool
        );
    }

    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        workerThreadPool.start();
        rotatingTimingWheelThread.start();
        taskReceiver.start();
        workerRegistry.register(currentWorker);
    }

    @Override
    public void close() {
        Throwables.caught(workerRegistry::close);
        Throwables.caught(taskReceiver::close);
        Throwables.caught(() -> rotatingTimingWheelThread.doStop(1000));
        Throwables.caught(workerThreadPool::close);
    }

    // ----------------------------------------------------------------------------------------builder

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Worker currentWorker;
        private int maximumPoolSize;
        private int keepAliveTimeSeconds;
        private SupervisorService SupervisorServiceClient;
        private WorkerRegistry workerRegistry;
        private TaskReceiver taskReceiver;

        private Builder() {
        }

        public Builder currentWorker(Worker currentWorker) {
            this.currentWorker = currentWorker;
            return this;
        }

        public Builder maximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
            return this;
        }

        public Builder keepAliveTimeSeconds(int keepAliveTimeSeconds) {
            this.keepAliveTimeSeconds = keepAliveTimeSeconds;
            return this;
        }

        public Builder SupervisorServiceClient(SupervisorService SupervisorServiceClient) {
            this.SupervisorServiceClient = SupervisorServiceClient;
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
                currentWorker, maximumPoolSize, keepAliveTimeSeconds,
                SupervisorServiceClient, workerRegistry, taskReceiver
            );
        }
    }

}

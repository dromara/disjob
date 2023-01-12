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
    private final WorkerHeartbeatThread workerHeartbeatThread;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private WorkerStartup(Worker currentWorker,
                          int maximumPoolSize,
                          int keepAliveTimeSeconds,
                          SupervisorService supervisorClient,
                          WorkerRegistry workerRegistry,
                          TaskReceiver taskReceiver) {
        Assert.notNull(currentWorker, "Current worker cannot null.");
        Assert.isTrue(maximumPoolSize > 0, "Maximum pool size must be greater zero.");
        Assert.isTrue(keepAliveTimeSeconds > 0, "Keep alive time seconds must be greater zero.");
        Assert.notNull(supervisorClient, "Supervisor client cannot null.");
        Assert.notNull(workerRegistry, "Server registry cannot null.");
        Assert.notNull(taskReceiver, "Task receiver cannot null.");

        this.currentWorker = currentWorker;
        this.workerThreadPool = new WorkerThreadPool(maximumPoolSize, keepAliveTimeSeconds, supervisorClient);
        this.workerRegistry = workerRegistry;
        this.taskReceiver = taskReceiver;
        this.workerHeartbeatThread = new WorkerHeartbeatThread(workerRegistry, taskReceiver.getTimingWheel(), workerThreadPool);
    }

    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        workerThreadPool.start();
        workerHeartbeatThread.start();
        taskReceiver.start();
        workerRegistry.register(currentWorker);
    }

    @Override
    public void close() {
        Throwables.caught(workerRegistry::close);
        Throwables.caught(taskReceiver::close);
        Throwables.caught(() -> workerHeartbeatThread.doStop(1000));
        Throwables.caught(workerThreadPool::close);
    }

    // ----------------------------------------------------------------------------------------builder

    public static WorkerStartup.WorkerStartupBuilder builder() {
        return new WorkerStartup.WorkerStartupBuilder();
    }

    public static class WorkerStartupBuilder {
        private Worker currentWorker;
        private int maximumPoolSize;
        private int keepAliveTimeSeconds;
        private SupervisorService supervisorClient;
        private WorkerRegistry workerRegistry;
        private TaskReceiver taskReceiver;

        private WorkerStartupBuilder() {
        }

        public WorkerStartup.WorkerStartupBuilder currentWorker(Worker currentWorker) {
            this.currentWorker = currentWorker;
            return this;
        }

        public WorkerStartup.WorkerStartupBuilder maximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
            return this;
        }

        public WorkerStartup.WorkerStartupBuilder keepAliveTimeSeconds(int keepAliveTimeSeconds) {
            this.keepAliveTimeSeconds = keepAliveTimeSeconds;
            return this;
        }

        public WorkerStartup.WorkerStartupBuilder supervisorClient(SupervisorService supervisorClient) {
            this.supervisorClient = supervisorClient;
            return this;
        }

        public WorkerStartup.WorkerStartupBuilder workerRegistry(WorkerRegistry workerRegistry) {
            this.workerRegistry = workerRegistry;
            return this;
        }

        public WorkerStartup.WorkerStartupBuilder taskReceiver(TaskReceiver taskReceiver) {
            this.taskReceiver = taskReceiver;
            return this;
        }

        public WorkerStartup build() {
            return new WorkerStartup(currentWorker, maximumPoolSize, keepAliveTimeSeconds, supervisorClient, workerRegistry, taskReceiver);
        }
    }

}

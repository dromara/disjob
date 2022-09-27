package cn.ponfee.scheduler.worker;

import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.dispatch.TaskReceiver;
import cn.ponfee.scheduler.registry.ServerRegistry;
import cn.ponfee.scheduler.worker.base.WorkerThreadPool;
import cn.ponfee.scheduler.worker.client.WorkerClient;
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
    private final ServerRegistry<Worker, Supervisor> workerRegistry;
    private final TaskReceiver taskReceiver;
    private final WorkerHeartbeatThread workerHeartbeatThread;

    private final AtomicBoolean start = new AtomicBoolean(false);

    private WorkerStartup(int maximumPoolSize,
                          int keepAliveTimeSeconds,
                          WorkerClient workerClient,
                          Worker currentWorker,
                          ServerRegistry<Worker, Supervisor> workerRegistry,
                          TaskReceiver taskReceiver) {
        Assert.isTrue(maximumPoolSize > 0, "Maximum pool size must be greater zero.");
        Assert.isTrue(keepAliveTimeSeconds > 0, "Keep alive time seconds must be greater zero.");
        Assert.notNull(workerClient, "Worker client cannot null.");
        Assert.notNull(currentWorker, "Current worker cannot null.");
        Assert.notNull(workerRegistry, "Server registry cannot null.");
        Assert.notNull(taskReceiver, "Task receiver cannot null.");

        this.workerThreadPool = new WorkerThreadPool(maximumPoolSize, keepAliveTimeSeconds, workerClient);
        this.currentWorker = currentWorker;
        this.workerRegistry = workerRegistry;
        this.taskReceiver = taskReceiver;
        this.workerHeartbeatThread = new WorkerHeartbeatThread(workerRegistry, taskReceiver.getTimingWheel(), workerThreadPool);
    }

    public void start() {
        if (!start.compareAndSet(false, true)) {
            return;
        }
        workerThreadPool.start();
        workerHeartbeatThread.start();
        taskReceiver.start();
        workerRegistry.register(currentWorker);
    }

    @Override
    public void close() {
        workerRegistry.close();
        taskReceiver.close();
        workerHeartbeatThread.doStop(1000);
        workerThreadPool.close();
    }

    // -----------------------------------------------------------------Builder

    public static WorkerStartup.WorkerStartupBuilder builder() {
        return new WorkerStartup.WorkerStartupBuilder();
    }

    public static class WorkerStartupBuilder {
        private int maximumPoolSize;
        private int keepAliveTimeSeconds;
        private WorkerClient workerClient;
        private Worker currentWorker;
        private ServerRegistry<Worker, Supervisor> workerRegistry;
        private TaskReceiver taskReceiver;

        public WorkerStartup.WorkerStartupBuilder maximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
            return this;
        }

        public WorkerStartup.WorkerStartupBuilder keepAliveTimeSeconds(int keepAliveTimeSeconds) {
            this.keepAliveTimeSeconds = keepAliveTimeSeconds;
            return this;
        }

        public WorkerStartup.WorkerStartupBuilder workerClient(WorkerClient workerClient) {
            this.workerClient = workerClient;
            return this;
        }

        public WorkerStartup.WorkerStartupBuilder currentWorker(Worker currentWorker) {
            this.currentWorker = currentWorker;
            return this;
        }

        public WorkerStartup.WorkerStartupBuilder workerRegistry(ServerRegistry<Worker, Supervisor> workerRegistry) {
            this.workerRegistry = workerRegistry;
            return this;
        }

        public WorkerStartup.WorkerStartupBuilder taskReceiver(TaskReceiver taskReceiver) {
            this.taskReceiver = taskReceiver;
            return this;
        }

        public WorkerStartup build() {
            return new WorkerStartup(maximumPoolSize, keepAliveTimeSeconds, workerClient, currentWorker, workerRegistry, taskReceiver);
        }
    }

}

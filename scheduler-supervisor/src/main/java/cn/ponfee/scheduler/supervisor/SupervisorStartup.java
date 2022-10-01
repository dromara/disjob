package cn.ponfee.scheduler.supervisor;

import cn.ponfee.scheduler.common.base.IdGenerator;
import cn.ponfee.scheduler.common.lock.DoInLocked;
import cn.ponfee.scheduler.common.util.ClassUtils;
import cn.ponfee.scheduler.common.util.Networks;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.dispatch.TaskDispatcher;
import cn.ponfee.scheduler.registry.ServerRegistry;
import cn.ponfee.scheduler.supervisor.manager.JobManager;
import org.springframework.util.Assert;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Startup supervisor.
 *
 * @author Ponfee
 */
public class SupervisorStartup implements AutoCloseable {

    private final Supervisor currentSupervisor;
    private final ScanJobHeartbeatThread scanJobHeartbeatThread;
    private final ScanTrackHeartbeatThread scanTrackHeartbeatThread;
    private final TaskDispatcher taskDispatcher;
    private final ServerRegistry<Supervisor, Worker> supervisorRegistry;

    private final AtomicBoolean start = new AtomicBoolean(false);

    private SupervisorStartup(int port,
                              int jobHeartbeatIntervalSeconds,
                              int trackHeartbeatIntervalSeconds,
                              ServerRegistry<Supervisor, Worker> supervisorRegistry,
                              JobManager jobManager,
                              DoInLocked scanJobLocked,
                              DoInLocked scanTrackLocked,
                              IdGenerator idGenerator,
                              TaskDispatcher taskDispatcher) {
        Assert.isTrue(port > 0, "Port must be greater zero.");
        Assert.isTrue(jobHeartbeatIntervalSeconds > 0, "Job heart beat interval seconds must be greater zero.");
        Assert.isTrue(trackHeartbeatIntervalSeconds > 0, "Track heart beat interval seconds must be greater zero.");
        Assert.notNull(supervisorRegistry, "Supervisor registry cannot null.");
        Assert.notNull(jobManager, "Job manager cannot null.");
        Assert.notNull(scanJobLocked, "Scan job locked cannot null.");
        Assert.notNull(scanTrackLocked, "Scan track locked cannot null.");
        Assert.notNull(idGenerator, "Id generator cannot null.");
        Assert.notNull(taskDispatcher, "Task dispatcher cannot null.");

        Supervisor currentSupervisor = new Supervisor(Networks.getHostIp(), port);
        // inject current supervisor
        try {
            ClassUtils.invoke(Class.forName(Supervisor.class.getName() + "$Current"), "set", new Object[]{currentSupervisor});
        } catch (ClassNotFoundException e) {
            // cannot happen
            throw new AssertionError("Setting as current supervisor occur error.", e);
        }

        this.currentSupervisor = currentSupervisor;
        this.supervisorRegistry = supervisorRegistry;
        this.scanJobHeartbeatThread = new ScanJobHeartbeatThread(jobHeartbeatIntervalSeconds, scanJobLocked, jobManager, idGenerator);
        this.scanTrackHeartbeatThread = new ScanTrackHeartbeatThread(trackHeartbeatIntervalSeconds, scanTrackLocked, jobManager);
        this.taskDispatcher = taskDispatcher;
    }

    public void start() {
        if (!start.compareAndSet(false, true)) {
            return;
        }
        scanJobHeartbeatThread.start();
        scanTrackHeartbeatThread.start();
        supervisorRegistry.register(currentSupervisor);
    }

    @Override
    public void close() {
        supervisorRegistry.close();
        scanTrackHeartbeatThread.toStop();
        scanJobHeartbeatThread.toStop();
        taskDispatcher.close();

        scanTrackHeartbeatThread.doStop(1000);
        scanJobHeartbeatThread.doStop(1000);
    }

    // ----------------------------------------------------------------------------------------builder

    public static SupervisorStartup.SupervisorStartupBuilder builder() {
        return new SupervisorStartup.SupervisorStartupBuilder();
    }

    public static class SupervisorStartupBuilder {
        private int port;
        private int jobHeartbeatIntervalSeconds;
        private int trackHeartbeatIntervalSeconds;
        private ServerRegistry<Supervisor, Worker> supervisorRegistry;
        private JobManager jobManager;
        private DoInLocked scanJobLocked;
        private DoInLocked scanTrackLocked;
        private IdGenerator idGenerator;
        private TaskDispatcher taskDispatcher;

        public SupervisorStartupBuilder port(int port) {
            this.port = port;
            return this;
        }

        public SupervisorStartupBuilder jobHeartbeatIntervalSeconds(int jobHeartbeatIntervalSeconds) {
            this.jobHeartbeatIntervalSeconds = jobHeartbeatIntervalSeconds;
            return this;
        }

        public SupervisorStartupBuilder trackHeartbeatIntervalSeconds(int trackHeartbeatIntervalSeconds) {
            this.trackHeartbeatIntervalSeconds = trackHeartbeatIntervalSeconds;
            return this;
        }

        public SupervisorStartupBuilder supervisorRegistry(ServerRegistry<Supervisor, Worker> supervisorRegistry) {
            this.supervisorRegistry = supervisorRegistry;
            return this;
        }

        public SupervisorStartupBuilder jobManager(JobManager jobManager) {
            this.jobManager = jobManager;
            return this;
        }

        public SupervisorStartupBuilder scanJobLocked(DoInLocked scanJobLocked) {
            this.scanJobLocked = scanJobLocked;
            return this;
        }

        public SupervisorStartupBuilder scanTrackLocked(DoInLocked scanTrackLocked) {
            this.scanTrackLocked = scanTrackLocked;
            return this;
        }

        public SupervisorStartupBuilder idGenerator(IdGenerator idGenerator) {
            this.idGenerator = idGenerator;
            return this;
        }

        public SupervisorStartupBuilder taskDispatcher(TaskDispatcher taskDispatcher) {
            this.taskDispatcher = taskDispatcher;
            return this;
        }

        public SupervisorStartup build() {
            return new SupervisorStartup(
                port, jobHeartbeatIntervalSeconds, trackHeartbeatIntervalSeconds,
                supervisorRegistry, jobManager, scanJobLocked, scanTrackLocked, idGenerator, taskDispatcher
            );
        }
    }

}

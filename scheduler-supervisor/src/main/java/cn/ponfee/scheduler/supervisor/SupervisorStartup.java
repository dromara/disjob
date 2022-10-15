package cn.ponfee.scheduler.supervisor;

import cn.ponfee.scheduler.common.base.exception.Throwables;
import cn.ponfee.scheduler.common.lock.DoInLocked;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.dispatch.TaskDispatcher;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
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
    private final SupervisorRegistry supervisorRegistry;

    private final AtomicBoolean start = new AtomicBoolean(false);

    private SupervisorStartup(Supervisor currentSupervisor,
                              int jobHeartbeatIntervalSeconds,
                              int trackHeartbeatIntervalSeconds,
                              SupervisorRegistry supervisorRegistry,
                              JobManager jobManager,
                              DoInLocked scanJobLocked,
                              DoInLocked scanTrackLocked,
                              TaskDispatcher taskDispatcher) {
        Assert.notNull(currentSupervisor, "Current supervisor cannot null.");
        Assert.isTrue(jobHeartbeatIntervalSeconds > 0, "Job heart beat interval seconds must be greater zero.");
        Assert.isTrue(trackHeartbeatIntervalSeconds > 0, "Track heart beat interval seconds must be greater zero.");
        Assert.notNull(supervisorRegistry, "Supervisor registry cannot null.");
        Assert.notNull(jobManager, "Job manager cannot null.");
        Assert.notNull(scanJobLocked, "Scan job locked cannot null.");
        Assert.notNull(scanTrackLocked, "Scan track locked cannot null.");
        Assert.notNull(taskDispatcher, "Task dispatcher cannot null.");

        this.currentSupervisor = currentSupervisor;
        this.supervisorRegistry = supervisorRegistry;
        this.scanJobHeartbeatThread = new ScanJobHeartbeatThread(jobHeartbeatIntervalSeconds, scanJobLocked, jobManager);
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
        Throwables.cached(supervisorRegistry::close);
        Throwables.cached(scanTrackHeartbeatThread::toStop);
        Throwables.cached(scanJobHeartbeatThread::toStop);
        Throwables.cached(taskDispatcher::close);
        Throwables.cached(() -> scanTrackHeartbeatThread.doStop(1000));
        Throwables.cached(() -> scanJobHeartbeatThread.doStop(1000));
    }

    // ----------------------------------------------------------------------------------------builder

    public static SupervisorStartup.SupervisorStartupBuilder builder() {
        return new SupervisorStartup.SupervisorStartupBuilder();
    }

    public static class SupervisorStartupBuilder {
        private Supervisor currentSupervisor;
        private int jobHeartbeatIntervalSeconds;
        private int trackHeartbeatIntervalSeconds;
        private SupervisorRegistry supervisorRegistry;
        private JobManager jobManager;
        private DoInLocked scanJobLocked;
        private DoInLocked scanTrackLocked;
        private TaskDispatcher taskDispatcher;

        public SupervisorStartupBuilder currentSupervisor(Supervisor currentSupervisor) {
            this.currentSupervisor = currentSupervisor;
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

        public SupervisorStartupBuilder supervisorRegistry(SupervisorRegistry supervisorRegistry) {
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

        public SupervisorStartupBuilder taskDispatcher(TaskDispatcher taskDispatcher) {
            this.taskDispatcher = taskDispatcher;
            return this;
        }

        public SupervisorStartup build() {
            return new SupervisorStartup(
                currentSupervisor,jobHeartbeatIntervalSeconds, trackHeartbeatIntervalSeconds,
                supervisorRegistry, jobManager, scanJobLocked, scanTrackLocked, taskDispatcher
            );
        }
    }

}

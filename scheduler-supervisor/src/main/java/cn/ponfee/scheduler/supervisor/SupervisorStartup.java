/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

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

    private final AtomicBoolean started = new AtomicBoolean(false);

    private SupervisorStartup(Supervisor currentSupervisor,
                              int jobHeartbeatIntervalMs,
                              int trackHeartbeatIntervalMs,
                              SupervisorRegistry supervisorRegistry,
                              JobManager jobManager,
                              DoInLocked scanJobLocked,
                              DoInLocked scanTrackLocked,
                              TaskDispatcher taskDispatcher) {
        Assert.notNull(currentSupervisor, "Current supervisor cannot null.");
        Assert.isTrue(jobHeartbeatIntervalMs > 0, "Job heart beat interval milliseconds must be greater zero.");
        Assert.isTrue(trackHeartbeatIntervalMs > 0, "Track heart beat interval milliseconds must be greater zero.");
        Assert.notNull(supervisorRegistry, "Supervisor registry cannot null.");
        Assert.notNull(jobManager, "Job manager cannot null.");
        Assert.notNull(scanJobLocked, "Scan job locked cannot null.");
        Assert.notNull(scanTrackLocked, "Scan track locked cannot null.");
        Assert.notNull(taskDispatcher, "Task dispatcher cannot null.");

        this.currentSupervisor = currentSupervisor;
        this.supervisorRegistry = supervisorRegistry;
        this.scanJobHeartbeatThread = new ScanJobHeartbeatThread(jobHeartbeatIntervalMs, scanJobLocked, jobManager);
        this.scanTrackHeartbeatThread = new ScanTrackHeartbeatThread(trackHeartbeatIntervalMs, scanTrackLocked, jobManager);
        this.taskDispatcher = taskDispatcher;
    }

    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        scanJobHeartbeatThread.start();
        scanTrackHeartbeatThread.start();
        supervisorRegistry.register(currentSupervisor);
    }

    @Override
    public void close() {
        Throwables.caught(supervisorRegistry::close);
        Throwables.caught(scanTrackHeartbeatThread::toStop);
        Throwables.caught(scanJobHeartbeatThread::toStop);
        Throwables.caught(taskDispatcher::close);
        Throwables.caught(() -> scanTrackHeartbeatThread.doStop(1000));
        Throwables.caught(() -> scanJobHeartbeatThread.doStop(1000));
    }

    // ----------------------------------------------------------------------------------------builder

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Supervisor currentSupervisor;
        private int jobHeartbeatIntervalMs;
        private int trackHeartbeatIntervalMs;
        private SupervisorRegistry supervisorRegistry;
        private JobManager jobManager;
        private DoInLocked scanJobLocked;
        private DoInLocked scanTrackLocked;
        private TaskDispatcher taskDispatcher;

        private Builder() {
        }

        public Builder currentSupervisor(Supervisor currentSupervisor) {
            this.currentSupervisor = currentSupervisor;
            return this;
        }

        public Builder jobHeartbeatIntervalMs(int jobHeartbeatIntervalMs) {
            this.jobHeartbeatIntervalMs = jobHeartbeatIntervalMs;
            return this;
        }

        public Builder trackHeartbeatIntervalMs(int trackHeartbeatIntervalMs) {
            this.trackHeartbeatIntervalMs = trackHeartbeatIntervalMs;
            return this;
        }

        public Builder supervisorRegistry(SupervisorRegistry supervisorRegistry) {
            this.supervisorRegistry = supervisorRegistry;
            return this;
        }

        public Builder jobManager(JobManager jobManager) {
            this.jobManager = jobManager;
            return this;
        }

        public Builder scanJobLocked(DoInLocked scanJobLocked) {
            this.scanJobLocked = scanJobLocked;
            return this;
        }

        public Builder scanTrackLocked(DoInLocked scanTrackLocked) {
            this.scanTrackLocked = scanTrackLocked;
            return this;
        }

        public Builder taskDispatcher(TaskDispatcher taskDispatcher) {
            this.taskDispatcher = taskDispatcher;
            return this;
        }

        public SupervisorStartup build() {
            return new SupervisorStartup(
                currentSupervisor,jobHeartbeatIntervalMs, trackHeartbeatIntervalMs,
                supervisorRegistry, jobManager, scanJobLocked, scanTrackLocked, taskDispatcher
            );
        }
    }

}

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
import cn.ponfee.scheduler.supervisor.configuration.SupervisorProperties;
import cn.ponfee.scheduler.supervisor.manager.SchedulerJobManager;
import cn.ponfee.scheduler.supervisor.thread.RunningTrackScanner;
import cn.ponfee.scheduler.supervisor.thread.TriggeringJobScanner;
import cn.ponfee.scheduler.supervisor.thread.WaitingTrackScanner;
import org.springframework.util.Assert;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Startup supervisor.
 *
 * @author Ponfee
 */
public class SupervisorStartup implements AutoCloseable {

    private final Supervisor currentSupervisor;
    private final TriggeringJobScanner triggeringJobScanner;
    private final WaitingTrackScanner waitingTrackScanner;
    private final RunningTrackScanner runningTrackScanner;
    private final TaskDispatcher taskDispatcher;
    private final SupervisorRegistry supervisorRegistry;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private SupervisorStartup(Supervisor currentSupervisor,
                              SupervisorProperties supervisorConfig,
                              SupervisorRegistry supervisorRegistry,
                              SchedulerJobManager schedulerJobManager,
                              DoInLocked scanTriggeringJobLocker,
                              DoInLocked scanWaitingTrackLocker,
                              DoInLocked scanRunningTrackLocker,
                              TaskDispatcher taskDispatcher) {
        Assert.notNull(currentSupervisor, "Current supervisor cannot null.");
        Assert.notNull(supervisorConfig, "Supervisor config cannot null.");
        Assert.notNull(supervisorRegistry, "Supervisor registry cannot null.");
        Assert.notNull(schedulerJobManager, "Scheduler job manager cannot null.");
        Assert.notNull(scanTriggeringJobLocker, "Scan triggering job locker cannot null.");
        Assert.notNull(scanWaitingTrackLocker, "Scan waiting track locker cannot null.");
        Assert.notNull(scanRunningTrackLocker, "Scan running track locker cannot null.");
        Assert.notNull(taskDispatcher, "Task dispatcher cannot null.");
        supervisorConfig.check();

        this.currentSupervisor = currentSupervisor;
        this.supervisorRegistry = supervisorRegistry;
        this.triggeringJobScanner = new TriggeringJobScanner(
            supervisorConfig.getScanTriggeringJobPeriodMs(), scanTriggeringJobLocker, schedulerJobManager
        );
        this.waitingTrackScanner = new WaitingTrackScanner(
            supervisorConfig.getScanWaitingTrackPeriodMs(), scanWaitingTrackLocker, schedulerJobManager
        );
        this.runningTrackScanner = new RunningTrackScanner(
            supervisorConfig.getScanRunningTrackPeriodMs(), scanRunningTrackLocker, schedulerJobManager
        );
        this.taskDispatcher = taskDispatcher;
    }

    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        triggeringJobScanner.start();
        waitingTrackScanner.start();
        runningTrackScanner.start();
        supervisorRegistry.register(currentSupervisor);
    }

    @Override
    public void close() {
        Throwables.caught(supervisorRegistry::close);
        Throwables.caught(runningTrackScanner::toStop);
        Throwables.caught(waitingTrackScanner::toStop);
        Throwables.caught(triggeringJobScanner::toStop);
        Throwables.caught(taskDispatcher::close);
        Throwables.caught(() -> runningTrackScanner.doStop(1000));
        Throwables.caught(() -> waitingTrackScanner.doStop(1000));
        Throwables.caught(() -> triggeringJobScanner.doStop(1000));
    }

    // ----------------------------------------------------------------------------------------builder

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Supervisor currentSupervisor;
        private SupervisorProperties supervisorConfig;
        private SupervisorRegistry supervisorRegistry;
        private SchedulerJobManager schedulerJobManager;
        private DoInLocked scanTriggeringJobLocker;
        private DoInLocked scanWaitingTrackLocker;
        private DoInLocked scanRunningTrackLocker;
        private TaskDispatcher taskDispatcher;

        private Builder() {
        }

        public Builder currentSupervisor(Supervisor currentSupervisor) {
            this.currentSupervisor = currentSupervisor;
            return this;
        }

        public Builder supervisorConfig(SupervisorProperties supervisorConfig) {
            this.supervisorConfig = supervisorConfig;
            return this;
        }

        public Builder supervisorRegistry(SupervisorRegistry supervisorRegistry) {
            this.supervisorRegistry = supervisorRegistry;
            return this;
        }

        public Builder schedulerJobManager(SchedulerJobManager schedulerJobManager) {
            this.schedulerJobManager = schedulerJobManager;
            return this;
        }

        public Builder scanTriggeringJobLocker(DoInLocked scanTriggeringJobLocker) {
            this.scanTriggeringJobLocker = scanTriggeringJobLocker;
            return this;
        }

        public Builder scanWaitingTrackLocker(DoInLocked scanWaitingTrackLocker) {
            this.scanWaitingTrackLocker = scanWaitingTrackLocker;
            return this;
        }

        public Builder scanRunningTrackLocker(DoInLocked scanRunningTrackLocker) {
            this.scanRunningTrackLocker = scanRunningTrackLocker;
            return this;
        }

        public Builder taskDispatcher(TaskDispatcher taskDispatcher) {
            this.taskDispatcher = taskDispatcher;
            return this;
        }

        public SupervisorStartup build() {
            return new SupervisorStartup(
                currentSupervisor, supervisorConfig, supervisorRegistry, schedulerJobManager,
                scanTriggeringJobLocker, scanWaitingTrackLocker, scanRunningTrackLocker, taskDispatcher
            );
        }
    }

}

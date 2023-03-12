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
import cn.ponfee.scheduler.supervisor.thread.RunningInstanceScanner;
import cn.ponfee.scheduler.supervisor.thread.TriggeringJobScanner;
import cn.ponfee.scheduler.supervisor.thread.WaitingInstanceScanner;
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
    private final WaitingInstanceScanner waitingInstanceScanner;
    private final RunningInstanceScanner runningInstanceScanner;
    private final TaskDispatcher taskDispatcher;
    private final SupervisorRegistry supervisorRegistry;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private SupervisorStartup(Supervisor currentSupervisor,
                              SupervisorProperties supervisorConfig,
                              SupervisorRegistry supervisorRegistry,
                              SchedulerJobManager schedulerJobManager,
                              DoInLocked scanTriggeringJobLocker,
                              DoInLocked scanWaitingInstanceLocker,
                              DoInLocked scanRunningInstanceLocker,
                              TaskDispatcher taskDispatcher) {
        Assert.notNull(currentSupervisor, "Current supervisor cannot null.");
        Assert.notNull(supervisorConfig, "Supervisor config cannot null.");
        Assert.notNull(supervisorRegistry, "Supervisor registry cannot null.");
        Assert.notNull(schedulerJobManager, "Scheduler job manager cannot null.");
        Assert.notNull(scanTriggeringJobLocker, "Scan triggering job locker cannot null.");
        Assert.notNull(scanWaitingInstanceLocker, "Scan waiting instance locker cannot null.");
        Assert.notNull(scanRunningInstanceLocker, "Scan running instance locker cannot null.");
        Assert.notNull(taskDispatcher, "Task dispatcher cannot null.");
        supervisorConfig.check();

        this.currentSupervisor = currentSupervisor;
        this.supervisorRegistry = supervisorRegistry;
        this.triggeringJobScanner = new TriggeringJobScanner(
            supervisorConfig.getScanTriggeringJobPeriodMs(), scanTriggeringJobLocker, schedulerJobManager
        );
        this.waitingInstanceScanner = new WaitingInstanceScanner(
            supervisorConfig.getScanWaitingInstancePeriodMs(), scanWaitingInstanceLocker, schedulerJobManager
        );
        this.runningInstanceScanner = new RunningInstanceScanner(
            supervisorConfig.getScanRunningInstancePeriodMs(), scanRunningInstanceLocker, schedulerJobManager
        );
        this.taskDispatcher = taskDispatcher;
    }

    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        triggeringJobScanner.start();
        waitingInstanceScanner.start();
        runningInstanceScanner.start();
        supervisorRegistry.register(currentSupervisor);
    }

    @Override
    public void close() {
        Throwables.caught(supervisorRegistry::close);
        Throwables.caught(runningInstanceScanner::toStop);
        Throwables.caught(waitingInstanceScanner::toStop);
        Throwables.caught(triggeringJobScanner::toStop);
        Throwables.caught(taskDispatcher::close);
        Throwables.caught(runningInstanceScanner::close);
        Throwables.caught(waitingInstanceScanner::close);
        Throwables.caught(triggeringJobScanner::close);
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
        private DoInLocked scanWaitingInstanceLocker;
        private DoInLocked scanRunningInstanceLocker;
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

        public Builder scanWaitingInstanceLocker(DoInLocked scanWaitingInstanceLocker) {
            this.scanWaitingInstanceLocker = scanWaitingInstanceLocker;
            return this;
        }

        public Builder scanRunningInstanceLocker(DoInLocked scanRunningInstanceLocker) {
            this.scanRunningInstanceLocker = scanRunningInstanceLocker;
            return this;
        }

        public Builder taskDispatcher(TaskDispatcher taskDispatcher) {
            this.taskDispatcher = taskDispatcher;
            return this;
        }

        public SupervisorStartup build() {
            return new SupervisorStartup(
                currentSupervisor, supervisorConfig, supervisorRegistry, schedulerJobManager,
                scanTriggeringJobLocker, scanWaitingInstanceLocker, scanRunningInstanceLocker, taskDispatcher
            );
        }
    }

}

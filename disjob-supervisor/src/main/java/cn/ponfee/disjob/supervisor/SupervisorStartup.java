/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor;

import cn.ponfee.disjob.common.base.Startable;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.lock.DoInLocked;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.dispatch.TaskDispatcher;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.supervisor.configuration.SupervisorProperties;
import cn.ponfee.disjob.supervisor.manager.DistributedJobManager;
import cn.ponfee.disjob.supervisor.thread.RunningInstanceScanner;
import cn.ponfee.disjob.supervisor.thread.TriggeringJobScanner;
import cn.ponfee.disjob.supervisor.thread.WaitingInstanceScanner;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Startup supervisor.
 *
 * @author Ponfee
 */
public class SupervisorStartup implements Startable {

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
                              DistributedJobManager distributedJobManager,
                              DoInLocked scanTriggeringJobLocker,
                              DoInLocked scanWaitingInstanceLocker,
                              DoInLocked scanRunningInstanceLocker,
                              TaskDispatcher taskDispatcher) {
        Objects.requireNonNull(currentSupervisor, "Current supervisor cannot null.");
        Objects.requireNonNull(supervisorConfig, "Supervisor config cannot null.").check();
        Objects.requireNonNull(supervisorRegistry, "Supervisor registry cannot null.");
        Objects.requireNonNull(distributedJobManager, "Distributed job manager cannot null.");
        Objects.requireNonNull(scanTriggeringJobLocker, "Scan triggering job locker cannot null.");
        Objects.requireNonNull(scanWaitingInstanceLocker, "Scan waiting instance locker cannot null.");
        Objects.requireNonNull(scanRunningInstanceLocker, "Scan running instance locker cannot null.");
        Objects.requireNonNull(taskDispatcher, "Task dispatcher cannot null.");

        this.currentSupervisor = currentSupervisor;
        this.supervisorRegistry = supervisorRegistry;
        this.triggeringJobScanner = new TriggeringJobScanner(
            supervisorConfig.getScanTriggeringJobPeriodMs(),
            supervisorConfig.getProcessJobMaximumPoolSize(),
            scanTriggeringJobLocker,
            distributedJobManager
        );
        this.waitingInstanceScanner = new WaitingInstanceScanner(
            supervisorConfig.getScanWaitingInstancePeriodMs(),
            scanWaitingInstanceLocker,
            distributedJobManager
        );
        this.runningInstanceScanner = new RunningInstanceScanner(
            supervisorConfig.getScanRunningInstancePeriodMs(),
            scanRunningInstanceLocker,
            distributedJobManager
        );
        this.taskDispatcher = taskDispatcher;
    }

    @Override
    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        runningInstanceScanner.start();
        waitingInstanceScanner.start();
        triggeringJobScanner.start();
        supervisorRegistry.register(currentSupervisor);
    }

    @Override
    public void stop() {
        ThrowingRunnable.caught(supervisorRegistry::close);
        ThrowingRunnable.caught(runningInstanceScanner::toStop);
        ThrowingRunnable.caught(waitingInstanceScanner::toStop);
        ThrowingRunnable.caught(triggeringJobScanner::toStop);
        ThrowingRunnable.caught(taskDispatcher::close);
        ThrowingRunnable.caught(triggeringJobScanner::close);
        ThrowingRunnable.caught(waitingInstanceScanner::close);
        ThrowingRunnable.caught(runningInstanceScanner::close);
    }

    // ----------------------------------------------------------------------------------------builder

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Supervisor currentSupervisor;
        private SupervisorProperties supervisorConfig;
        private SupervisorRegistry supervisorRegistry;
        private DistributedJobManager distributedJobManager;
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

        public Builder distributedJobManager(DistributedJobManager distributedJobManager) {
            this.distributedJobManager = distributedJobManager;
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
                currentSupervisor,
                supervisorConfig,
                supervisorRegistry,
                distributedJobManager,
                scanTriggeringJobLocker,
                scanWaitingInstanceLocker,
                scanRunningInstanceLocker,
                taskDispatcher
            );
        }
    }

}

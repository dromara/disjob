/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.supervisor;

import cn.ponfee.disjob.common.base.Startable;
import cn.ponfee.disjob.common.concurrent.TripState;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.lock.LockTemplate;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.dispatch.TaskDispatcher;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.supervisor.component.DistributedJobManager;
import cn.ponfee.disjob.supervisor.component.DistributedJobQuerier;
import cn.ponfee.disjob.supervisor.configuration.SupervisorProperties;
import cn.ponfee.disjob.supervisor.thread.RunningInstanceScanner;
import cn.ponfee.disjob.supervisor.thread.TriggeringJobScanner;
import cn.ponfee.disjob.supervisor.thread.WaitingInstanceScanner;
import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Startup supervisor.
 *
 * @author Ponfee
 */
public class SupervisorStartup implements Startable {

    private static final Logger LOG = LoggerFactory.getLogger(SupervisorStartup.class);

    private final Supervisor.Current currentSupervisor;
    private final TriggeringJobScanner triggeringJobScanner;
    private final WaitingInstanceScanner waitingInstanceScanner;
    private final RunningInstanceScanner runningInstanceScanner;
    private final TaskDispatcher taskDispatcher;
    private final SupervisorRegistry supervisorRegistry;

    private final TripState state = TripState.create();

    private SupervisorStartup(Supervisor.Current currentSupervisor,
                              SupervisorProperties supervisorProperties,
                              SupervisorRegistry supervisorRegistry,
                              DistributedJobManager distributedJobManager,
                              DistributedJobQuerier distributedJobQuerier,
                              LockTemplate scanTriggeringJobLocker,
                              LockTemplate scanWaitingInstanceLocker,
                              LockTemplate scanRunningInstanceLocker,
                              TaskDispatcher taskDispatcher) {
        Objects.requireNonNull(currentSupervisor, "Current supervisor cannot null.");
        Objects.requireNonNull(supervisorProperties, "Supervisor properties cannot null.").check();
        Objects.requireNonNull(supervisorRegistry, "Supervisor registry cannot null.");
        Objects.requireNonNull(distributedJobManager, "Distributed job manager cannot null.");
        Objects.requireNonNull(scanTriggeringJobLocker, "Scan triggering job locker cannot null.");
        Objects.requireNonNull(scanWaitingInstanceLocker, "Scan waiting instance locker cannot null.");
        Objects.requireNonNull(scanRunningInstanceLocker, "Scan running instance locker cannot null.");
        Objects.requireNonNull(taskDispatcher, "Task dispatcher cannot null.");

        this.currentSupervisor = currentSupervisor;
        this.supervisorRegistry = supervisorRegistry;
        this.triggeringJobScanner = new TriggeringJobScanner(
            supervisorProperties,
            scanTriggeringJobLocker,
            distributedJobManager,
            distributedJobQuerier
        );
        this.waitingInstanceScanner = new WaitingInstanceScanner(
            supervisorProperties.getScanWaitingInstancePeriodMs(),
            scanWaitingInstanceLocker,
            distributedJobManager,
            distributedJobQuerier
        );
        this.runningInstanceScanner = new RunningInstanceScanner(
            supervisorProperties.getScanRunningInstancePeriodMs(),
            scanRunningInstanceLocker,
            distributedJobManager,
            distributedJobQuerier
        );
        this.taskDispatcher = taskDispatcher;
    }

    @Override
    public void start() {
        if (!state.start()) {
            LOG.warn("Supervisor startup already started.");
            return;
        }

        Stopwatch stopwatch = Stopwatch.createStarted();
        LOG.info("Supervisor start begin: {}", currentSupervisor);
        waitingInstanceScanner.start();
        runningInstanceScanner.start();
        triggeringJobScanner.start();
        supervisorRegistry.register(currentSupervisor);
        LOG.info("Supervisor start end: {}, {}", currentSupervisor, stopwatch.stop());
    }

    @Override
    public void stop() {
        if (!state.stop()) {
            LOG.warn("Supervisor startup already Stopped.");
            return;
        }

        Stopwatch stopwatch = Stopwatch.createStarted();
        LOG.info("Supervisor stop begin: {}", currentSupervisor);
        ThrowingRunnable.doCaught(supervisorRegistry::close);
        ThrowingRunnable.doCaught(triggeringJobScanner::toStop);
        ThrowingRunnable.doCaught(runningInstanceScanner::toStop);
        ThrowingRunnable.doCaught(waitingInstanceScanner::toStop);
        ThrowingRunnable.doCaught(taskDispatcher::close);
        ThrowingRunnable.doCaught(triggeringJobScanner::close);
        ThrowingRunnable.doCaught(runningInstanceScanner::close);
        ThrowingRunnable.doCaught(waitingInstanceScanner::close);
        LOG.info("Supervisor stop end: {}, {}", currentSupervisor, stopwatch.stop());
    }

    // ----------------------------------------------------------------------------------------builder

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Supervisor.Current currentSupervisor;
        private SupervisorProperties supervisorProperties;
        private SupervisorRegistry supervisorRegistry;
        private DistributedJobManager distributedJobManager;
        private DistributedJobQuerier distributedJobQuerier;
        private LockTemplate scanTriggeringJobLocker;
        private LockTemplate scanWaitingInstanceLocker;
        private LockTemplate scanRunningInstanceLocker;
        private TaskDispatcher taskDispatcher;

        private Builder() {
        }

        public Builder currentSupervisor(Supervisor.Current currentSupervisor) {
            this.currentSupervisor = currentSupervisor;
            return this;
        }

        public Builder supervisorProperties(SupervisorProperties supervisorProperties) {
            this.supervisorProperties = supervisorProperties;
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

        public Builder distributedJobQuerier(DistributedJobQuerier distributedJobQuerier) {
            this.distributedJobQuerier = distributedJobQuerier;
            return this;
        }

        public Builder scanTriggeringJobLocker(LockTemplate scanTriggeringJobLocker) {
            this.scanTriggeringJobLocker = scanTriggeringJobLocker;
            return this;
        }

        public Builder scanWaitingInstanceLocker(LockTemplate scanWaitingInstanceLocker) {
            this.scanWaitingInstanceLocker = scanWaitingInstanceLocker;
            return this;
        }

        public Builder scanRunningInstanceLocker(LockTemplate scanRunningInstanceLocker) {
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
                supervisorProperties,
                supervisorRegistry,
                distributedJobManager,
                distributedJobQuerier,
                scanTriggeringJobLocker,
                scanWaitingInstanceLocker,
                scanRunningInstanceLocker,
                taskDispatcher
            );
        }
    }

}

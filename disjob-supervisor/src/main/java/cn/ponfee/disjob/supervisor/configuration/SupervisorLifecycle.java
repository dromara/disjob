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

package cn.ponfee.disjob.supervisor.configuration;

import cn.ponfee.disjob.common.concurrent.TripState;
import cn.ponfee.disjob.common.lock.LockTemplate;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.dispatch.TaskDispatcher;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.supervisor.SupervisorStartup;
import cn.ponfee.disjob.supervisor.component.DistributedJobManager;
import cn.ponfee.disjob.supervisor.component.DistributedJobQuerier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;

import static cn.ponfee.disjob.supervisor.base.SupervisorConstants.*;

/**
 * Supervisor lifecycle
 *
 * https://www.cnblogs.com/deityjian/p/11296846.html
 *
 * InitializingBean#afterPropertiesSet -> SmartLifecycle#start -> SmartLifecycle#stop -> DisposableBean#destroy
 *
 * @author Ponfee
 */
public class SupervisorLifecycle implements SmartLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(SupervisorLifecycle.class);

    private final TripState state = TripState.create();
    private final SupervisorStartup supervisorStartup;

    public SupervisorLifecycle(Supervisor.Local localSupervisor,
                               SupervisorProperties supervisorProperties,
                               SupervisorRegistry supervisorRegistry,
                               DistributedJobManager distributedJobManager,
                               DistributedJobQuerier distributedJobQuerier,
                               @Qualifier(SPRING_BEAN_NAME_SCAN_TRIGGERING_JOB_LOCKER) LockTemplate scanTriggeringJobLocker,
                               @Qualifier(SPRING_BEAN_NAME_SCAN_WAITING_INSTANCE_LOCKER) LockTemplate scanWaitingInstanceLocker,
                               @Qualifier(SPRING_BEAN_NAME_SCAN_RUNNING_INSTANCE_LOCKER) LockTemplate scanRunningInstanceLocker,
                               TaskDispatcher taskDispatcher) {
        this.supervisorStartup = SupervisorStartup.builder()
            .localSupervisor(localSupervisor)
            .supervisorProperties(supervisorProperties)
            .supervisorRegistry(supervisorRegistry)
            .distributedJobManager(distributedJobManager)
            .distributedJobQuerier(distributedJobQuerier)
            .scanTriggeringJobLocker(scanTriggeringJobLocker)
            .scanWaitingInstanceLocker(scanWaitingInstanceLocker)
            .scanRunningInstanceLocker(scanRunningInstanceLocker)
            .taskDispatcher(taskDispatcher)
            .build();
    }

    @Override
    public boolean isRunning() {
        return state.isRunning();
    }

    @Override
    public void start() {
        if (!state.start()) {
            LOG.error("Disjob supervisor lifecycle already stated!");
        }

        supervisorStartup.start();
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void stop(Runnable callback) {
        if (!state.stop()) {
            LOG.error("Disjob supervisor lifecycle already stopped!");
        }

        supervisorStartup.stop();
        callback.run();
    }

    @Override
    public void stop() {
        stop(() -> {});
    }

    @Override
    public int getPhase() {
        return DEFAULT_PHASE;
    }

}

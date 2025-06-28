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

import cn.ponfee.disjob.common.lock.LockTemplate;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.dispatch.TaskDispatcher;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.supervisor.SupervisorStartup;
import cn.ponfee.disjob.supervisor.component.JobManager;
import cn.ponfee.disjob.supervisor.component.JobQuerier;
import cn.ponfee.disjob.supervisor.component.WorkerClient;
import cn.ponfee.disjob.supervisor.scanner.RunningInstanceScanner;
import cn.ponfee.disjob.supervisor.scanner.TriggeringJobScanner;
import cn.ponfee.disjob.supervisor.scanner.WaitingInstanceScanner;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;

import static cn.ponfee.disjob.supervisor.base.SupervisorConstants.*;

/**
 * Supervisor lifecycle
 * <p>https://www.cnblogs.com/deityjian/p/11296846.html
 * <p>InitializingBean#afterPropertiesSet -> SmartLifecycle#start -> SmartLifecycle#stop -> DisposableBean#destroy
 *
 * @author Ponfee
 */
class SupervisorLifecycle implements SmartLifecycle {

    private final SupervisorStartup supervisorStartup;

    @SuppressWarnings("all")
    SupervisorLifecycle(Supervisor.Local localSupervisor,
                        SupervisorRegistry supervisorRegistry,
                        TaskDispatcher taskDispatcher,
                        SupervisorProperties supervisorConf,
                        JobManager jobManager,
                        JobQuerier jobQuerier,
                        WorkerClient workerClient,
                        @Qualifier(SPRING_BEAN_NAME_SCAN_WAITING_INSTANCE_LOCKER) LockTemplate scanWaitingInstanceLocker,
                        @Qualifier(SPRING_BEAN_NAME_SCAN_RUNNING_INSTANCE_LOCKER) LockTemplate scanRunningInstanceLocker,
                        @Qualifier(SPRING_BEAN_NAME_SCAN_TRIGGERING_JOB_LOCKER) LockTemplate scanTriggeringJobLocker) {
        supervisorConf.check();
        this.supervisorStartup = new SupervisorStartup(
            localSupervisor,
            supervisorConf,
            supervisorRegistry,
            taskDispatcher,
            new WaitingInstanceScanner(supervisorConf, jobManager, jobQuerier, workerClient, scanWaitingInstanceLocker),
            new RunningInstanceScanner(supervisorConf, jobManager, jobQuerier, workerClient, scanRunningInstanceLocker),
            new TriggeringJobScanner  (supervisorConf, jobManager, jobQuerier, workerClient, scanTriggeringJobLocker)
        );
    }

    @Override
    public void start() {
        supervisorStartup.start();
    }

    @Override
    public void stop(Runnable callback) {
        supervisorStartup.stop();
        callback.run();
    }

    @Override
    public void stop() {
        stop(() -> {});
    }

    @Override
    public boolean isRunning() {
        return supervisorStartup.isRunning();
    }

    @Override
    public int getPhase() {
        return DEFAULT_PHASE;
    }

}

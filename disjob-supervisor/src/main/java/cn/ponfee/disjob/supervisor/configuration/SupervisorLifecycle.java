/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.configuration;

import cn.ponfee.disjob.common.lock.DoInLocked;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.dispatch.TaskDispatcher;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.supervisor.SupervisorStartup;
import cn.ponfee.disjob.supervisor.service.DistributedJobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;

import java.util.concurrent.atomic.AtomicBoolean;

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

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final SupervisorStartup supervisorStartup;

    public SupervisorLifecycle(Supervisor currentSupervisor,
                               SupervisorProperties supervisorProperties,
                               SupervisorRegistry supervisorRegistry,
                               DistributedJobManager distributedJobManager,
                               @Qualifier(SPRING_BEAN_NAME_SCAN_TRIGGERING_JOB_LOCKER) DoInLocked scanTriggeringJobLocker,
                               @Qualifier(SPRING_BEAN_NAME_SCAN_WAITING_INSTANCE_LOCKER) DoInLocked scanWaitingInstanceLocker,
                               @Qualifier(SPRING_BEAN_NAME_SCAN_RUNNING_INSTANCE_LOCKER) DoInLocked scanRunningInstanceLocker,
                               TaskDispatcher taskDispatcher) {
        this.supervisorStartup = SupervisorStartup.builder()
            .currentSupervisor(currentSupervisor)
            .supervisorProperties(supervisorProperties)
            .supervisorRegistry(supervisorRegistry)
            .distributedJobManager(distributedJobManager)
            .scanTriggeringJobLocker(scanTriggeringJobLocker)
            .scanWaitingInstanceLocker(scanWaitingInstanceLocker)
            .scanRunningInstanceLocker(scanRunningInstanceLocker)
            .taskDispatcher(taskDispatcher)
            .build();
    }

    @Override
    public boolean isRunning() {
        return started.get();
    }

    @Override
    public void start() {
        if (!started.compareAndSet(false, true)) {
            LOG.error("Disjob supervisor lifecycle already stated!");
        }

        LOG.info("Disjob supervisor launch begin...");
        supervisorStartup.start();
        LOG.info("Disjob supervisor launch end.");
    }

    @Override
    public void stop(Runnable callback) {
        if (!started.compareAndSet(true, false)) {
            LOG.error("Disjob supervisor lifecycle already stopped!");
        }

        LOG.info("Disjob supervisor stop begin...");
        supervisorStartup.stop();
        LOG.info("Disjob supervisor stop end.");

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

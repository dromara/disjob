/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.configuration;

import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.lock.DoInLocked;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.dispatch.TaskDispatcher;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.supervisor.SupervisorStartup;
import cn.ponfee.disjob.supervisor.manager.DistributedJobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.core.Ordered;

import static cn.ponfee.disjob.supervisor.base.SupervisorConstants.*;

/**
 * Supervisor startup runner.
 *
 * @author Ponfee
 */
@AutoConfigureOrder(SupervisorStartupRunner.ORDERED)
public class SupervisorStartupRunner implements ApplicationRunner, DisposableBean, Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(SupervisorStartupRunner.class);
    static final int ORDERED = Ordered.LOWEST_PRECEDENCE;

    private final SupervisorStartup supervisorStartup;

    public SupervisorStartupRunner(Supervisor currentSupervisor,
                                   SupervisorProperties supervisorConfig,
                                   DistributedJobManager distributedJobManager,
                                   @Qualifier(SPRING_BEAN_NAME_SCAN_TRIGGERING_JOB_LOCKER) DoInLocked scanTriggeringJobLocker,
                                   @Qualifier(SPRING_BEAN_NAME_SCAN_WAITING_INSTANCE_LOCKER) DoInLocked scanWaitingInstanceLocker,
                                   @Qualifier(SPRING_BEAN_NAME_SCAN_RUNNING_INSTANCE_LOCKER) DoInLocked scanRunningInstanceLocker,
                                   SupervisorRegistry supervisorRegistry,
                                   TaskDispatcher taskDispatcher) {
        this.supervisorStartup = SupervisorStartup.builder()
            .currentSupervisor(currentSupervisor)
            .supervisorConfig(supervisorConfig)
            .supervisorRegistry(supervisorRegistry)
            .distributedJobManager(distributedJobManager)
            .scanTriggeringJobLocker(scanTriggeringJobLocker)
            .scanWaitingInstanceLocker(scanWaitingInstanceLocker)
            .scanRunningInstanceLocker(scanRunningInstanceLocker)
            .taskDispatcher(taskDispatcher)
            .build();
    }

    @Override
    public void run(ApplicationArguments args) {
        LOG.info("Disjob supervisor launch begin...");
        // if the server also a worker, wait the worker registered
        ThrowingRunnable.run(() -> Thread.sleep(3000));
        supervisorStartup.start();
        LOG.info("Disjob supervisor launch end.");
    }

    @Override
    public void destroy() {
        LOG.info("Disjob supervisor stop begin...");
        supervisorStartup.stop();
        LOG.info("Disjob supervisor stop end.");
    }

    @Override
    public int getOrder() {
        return ORDERED;
    }
}

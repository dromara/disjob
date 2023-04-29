/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.configuration;

import cn.ponfee.scheduler.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.scheduler.common.lock.DoInLocked;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.dispatch.TaskDispatcher;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import cn.ponfee.scheduler.supervisor.SupervisorStartup;
import cn.ponfee.scheduler.supervisor.manager.SchedulerJobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.core.Ordered;

import static cn.ponfee.scheduler.supervisor.base.SupervisorConstants.*;

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
                                   SchedulerJobManager schedulerJobManager,
                                   @Qualifier(SPRING_BEAN_NAME_SCAN_TRIGGERING_JOB_LOCKER) DoInLocked scanTriggeringJobLocker,
                                   @Qualifier(SPRING_BEAN_NAME_SCAN_WAITING_INSTANCE_LOCKER) DoInLocked scanWaitingInstanceLocker,
                                   @Qualifier(SPRING_BEAN_NAME_SCAN_RUNNING_INSTANCE_LOCKER) DoInLocked scanRunningInstanceLocker,
                                   SupervisorRegistry supervisorRegistry,
                                   TaskDispatcher taskDispatcher) {
        this.supervisorStartup = SupervisorStartup.builder()
            .currentSupervisor(currentSupervisor)
            .supervisorConfig(supervisorConfig)
            .supervisorRegistry(supervisorRegistry)
            .schedulerJobManager(schedulerJobManager)
            .scanTriggeringJobLocker(scanTriggeringJobLocker)
            .scanWaitingInstanceLocker(scanWaitingInstanceLocker)
            .scanRunningInstanceLocker(scanRunningInstanceLocker)
            .taskDispatcher(taskDispatcher)
            .build();
    }

    @Override
    public void run(ApplicationArguments args) {
        LOG.info("Scheduler supervisor launch begin...");
        // if the server also a worker, wait the worker registered
        ThrowingRunnable.run(() -> Thread.sleep(3000));
        supervisorStartup.start();
        LOG.info("Scheduler supervisor launch end.");
    }

    @Override
    public void destroy() {
        LOG.info("Scheduler supervisor stop begin...");
        supervisorStartup.stop();
        LOG.info("Scheduler supervisor stop end.");
    }

    @Override
    public int getOrder() {
        return ORDERED;
    }
}

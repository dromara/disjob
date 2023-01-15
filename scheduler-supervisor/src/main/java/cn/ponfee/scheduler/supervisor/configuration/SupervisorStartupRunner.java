/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.configuration;

import cn.ponfee.scheduler.common.lock.DoInLocked;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.dispatch.TaskDispatcher;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import cn.ponfee.scheduler.supervisor.SupervisorStartup;
import cn.ponfee.scheduler.supervisor.manager.JobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import static cn.ponfee.scheduler.supervisor.base.SupervisorConstants.SPRING_BEAN_NAME_SCAN_JOB_LOCKED;
import static cn.ponfee.scheduler.supervisor.base.SupervisorConstants.SPRING_BEAN_NAME_SCAN_TRACK_LOCKED;

/**
 * Supervisor startup runner.
 *
 * @author Ponfee
 */
public class SupervisorStartupRunner implements ApplicationRunner, DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(SupervisorStartupRunner.class);

    private final SupervisorStartup supervisorStartup;

    public SupervisorStartupRunner(Supervisor currentSupervisor,
                                   SupervisorProperties config,
                                   JobManager jobManager,
                                   @Qualifier(SPRING_BEAN_NAME_SCAN_JOB_LOCKED) DoInLocked scanJobLocked,
                                   @Qualifier(SPRING_BEAN_NAME_SCAN_TRACK_LOCKED) DoInLocked scanTrackLocked,
                                   SupervisorRegistry supervisorRegistry,
                                   TaskDispatcher taskDispatcher) {
        this.supervisorStartup = SupervisorStartup.builder()
            .currentSupervisor(currentSupervisor)
            .jobHeartbeatPeriodMs(config.getJobHeartbeatPeriodMs())
            .trackHeartbeatPeriodMs(config.getTrackHeartbeatPeriodMs())
            .supervisorRegistry(supervisorRegistry)
            .jobManager(jobManager)
            .scanJobLocked(scanJobLocked)
            .scanTrackLocked(scanTrackLocked)
            .taskDispatcher(taskDispatcher)
            .build();
    }

    @Override
    public void run(ApplicationArguments args) {
        LOG.info("Scheduler supervisor launch begin...");
        supervisorStartup.start();
        LOG.info("Scheduler supervisor launch end.");
    }

    @Override
    public void destroy() {
        LOG.info("Scheduler supervisor stop begin...");
        supervisorStartup.close();
        LOG.info("Scheduler supervisor stop end.");
    }

}

package cn.ponfee.scheduler.supervisor.samples.configuration;

import cn.ponfee.scheduler.common.base.IdGenerator;
import cn.ponfee.scheduler.common.lock.DoInLocked;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.dispatch.TaskDispatcher;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import cn.ponfee.scheduler.supervisor.SupervisorStartup;
import cn.ponfee.scheduler.supervisor.configuration.SupervisorProperties;
import cn.ponfee.scheduler.supervisor.manager.JobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import static cn.ponfee.scheduler.supervisor.base.SupervisorConstants.SPRING_BEAN_NAME_SCAN_JOB_LOCKED;
import static cn.ponfee.scheduler.supervisor.base.SupervisorConstants.SPRING_BEAN_NAME_SCAN_TRACK_LOCKED;

/**
 * Supervisor startup runner.
 *
 * @author Ponfee
 */
@Component
public class SupervisorStartupRunner implements ApplicationRunner, DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(SupervisorStartupRunner.class);

    private final SupervisorStartup supervisorStartup;

    public SupervisorStartupRunner(Supervisor currentSupervisor,
                                   SupervisorProperties properties,
                                   JobManager jobManager,
                                   IdGenerator idGenerator,
                                   @Qualifier(SPRING_BEAN_NAME_SCAN_JOB_LOCKED) DoInLocked scanJobLocked,
                                   @Qualifier(SPRING_BEAN_NAME_SCAN_TRACK_LOCKED) DoInLocked scanTrackLocked,
                                   SupervisorRegistry supervisorRegistry,
                                   TaskDispatcher taskDispatcher) {
        this.supervisorStartup = SupervisorStartup.builder()
            .currentSupervisor(currentSupervisor)
            .jobHeartbeatIntervalSeconds(properties.getJobHeartbeatIntervalSeconds())
            .trackHeartbeatIntervalSeconds(properties.getTrackHeartbeatIntervalSeconds())
            .supervisorRegistry(supervisorRegistry)
            .jobManager(jobManager)
            .scanJobLocked(scanJobLocked)
            .scanTrackLocked(scanTrackLocked)
            .idGenerator(idGenerator)
            .taskDispatcher(taskDispatcher)
            .build();
    }

    @Override
    public void run(ApplicationArguments args) {
        supervisorStartup.start();
    }

    @Override
    public void destroy() {
        LOG.info("Scheduler supervisor destroy start...");
        supervisorStartup.close();
        LOG.info("Scheduler supervisor destroy end.");
    }

}

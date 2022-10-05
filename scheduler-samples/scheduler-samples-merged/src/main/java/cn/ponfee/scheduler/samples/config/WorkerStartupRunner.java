package cn.ponfee.scheduler.samples.config;

import cn.ponfee.scheduler.core.base.SupervisorService;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.dispatch.TaskReceiver;
import cn.ponfee.scheduler.registry.WorkerRegistry;
import cn.ponfee.scheduler.worker.WorkerStartup;
import cn.ponfee.scheduler.worker.base.TaskTimingWheel;
import cn.ponfee.scheduler.worker.configuration.WorkerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

/**
 * Worker startup runner.
 *
 * @author Ponfee
 */
@Import(TaskTimingWheel.class)
@Component
public class WorkerStartupRunner implements ApplicationRunner, DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(WorkerStartupRunner.class);

    private final WorkerStartup workerStartup;

    public WorkerStartupRunner(Worker currentWorker,
                               WorkerProperties properties,
                               SupervisorService supervisorServiceClient, // JobManager
                               WorkerRegistry workerRegistry,
                               TaskReceiver taskReceiver) {
        this.workerStartup = WorkerStartup.builder()
            .currentWorker(currentWorker)
            .maximumPoolSize(properties.getMaximumPoolSize())
            .keepAliveTimeSeconds(properties.getKeepAliveTimeSeconds())
            .supervisorService(supervisorServiceClient)
            .workerRegistry(workerRegistry)
            .taskReceiver(taskReceiver)
            .build();
    }

    @Override
    public void run(ApplicationArguments args) {
        workerStartup.start();
    }

    @Override
    public void destroy() {
        LOG.info("Scheduler worker destroy start...");
        workerStartup.close();
        LOG.info("Scheduler worker destroy end.");
    }

}

package cn.ponfee.scheduler.samples.config;

import cn.ponfee.scheduler.core.base.SupervisorService;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.dispatch.TaskReceiver;
import cn.ponfee.scheduler.registry.WorkerRegistry;
import cn.ponfee.scheduler.worker.WorkerStartup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import static cn.ponfee.scheduler.core.base.JobConstants.WORKER_KEY_PREFIX;

/**
 * Worker startup runner.
 *
 * @author Ponfee
 */
@Component
public class WorkerStartupRunner implements ApplicationRunner, DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(WorkerStartupRunner.class);

    private final WorkerStartup workerStartup;

    public WorkerStartupRunner(Worker currentWorker,
                               @Value("${" + WORKER_KEY_PREFIX + ".maximum-pool-size:100}") int maximumPoolSize,
                               @Value("${" + WORKER_KEY_PREFIX + ".keep-alive-time-seconds:300}") int keepAliveTimeSeconds,
                               SupervisorService supervisorService, // JobManager
                               WorkerRegistry workerRegistry,
                               TaskReceiver taskReceiver) {
        this.workerStartup = WorkerStartup.builder()
            .currentWorker(currentWorker)
            .maximumPoolSize(maximumPoolSize)
            .keepAliveTimeSeconds(keepAliveTimeSeconds)
            .supervisorService(supervisorService)
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

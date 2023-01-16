/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.worker.configuration;

import cn.ponfee.scheduler.core.base.SupervisorService;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.dispatch.TaskReceiver;
import cn.ponfee.scheduler.registry.WorkerRegistry;
import cn.ponfee.scheduler.worker.WorkerStartup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.core.Ordered;

/**
 * Worker startup runner.
 *
 * @author Ponfee
 */
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
public class WorkerStartupRunner implements ApplicationRunner, DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(WorkerStartupRunner.class);

    private final WorkerStartup workerStartup;

    public WorkerStartupRunner(Worker currentWorker,
                               WorkerProperties properties,
                               // if current server also is a supervisor -> JobManager, else -> DiscoveryRestProxy.create()
                               SupervisorService supervisorClient,
                               WorkerRegistry workerRegistry,
                               TaskReceiver taskReceiver) {
        this.workerStartup = WorkerStartup.builder()
            .currentWorker(currentWorker)
            .maximumPoolSize(properties.getMaximumPoolSize())
            .keepAliveTimeSeconds(properties.getKeepAliveTimeSeconds())
            .supervisorClient(supervisorClient)
            .workerRegistry(workerRegistry)
            .taskReceiver(taskReceiver)
            .build();
    }

    @Override
    public void run(ApplicationArguments args) {
        LOG.info("Scheduler worker launch begin...");
        workerStartup.start();
        LOG.info("Scheduler worker launch end.");
    }

    @Override
    public void destroy() {
        LOG.info("Scheduler worker stop begin...");
        workerStartup.close();
        LOG.info("Scheduler worker stop end.");
    }

}

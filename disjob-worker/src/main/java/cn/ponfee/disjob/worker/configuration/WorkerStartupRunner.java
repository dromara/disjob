/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.worker.configuration;

import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.SupervisorService;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.dispatch.TaskReceiver;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.worker.WorkerStartup;
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
@AutoConfigureOrder(WorkerStartupRunner.ORDERED)
public class WorkerStartupRunner implements ApplicationRunner, DisposableBean, Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(WorkerStartupRunner.class);
    static final int ORDERED = Ordered.LOWEST_PRECEDENCE - 1;

    private final WorkerStartup workerStartup;

    public WorkerStartupRunner(Worker currentWorker,
                               RetryProperties retryProperties,
                               WorkerProperties workerProperties,
                               // if current server also is a supervisor -> JobManager, else -> DiscoveryRestProxy.create()
                               SupervisorService supervisorServiceClient,
                               WorkerRegistry workerRegistry,
                               TaskReceiver taskReceiver) {
        this.workerStartup = WorkerStartup.builder()
            .currentWorker(currentWorker)
            .retryProperties(retryProperties)
            .workerConfig(workerProperties)
            .supervisorServiceClient(supervisorServiceClient)
            .workerRegistry(workerRegistry)
            .taskReceiver(taskReceiver)
            .build();
    }

    @Override
    public void run(ApplicationArguments args) {
        LOG.info("Disjob worker launch begin...");
        workerStartup.start();
        LOG.info("Disjob worker launch end.");
    }

    @Override
    public void destroy() {
        LOG.info("Disjob worker stop begin...");
        workerStartup.stop();
        LOG.info("Disjob worker stop end.");
    }

    @Override
    public int getOrder() {
        return WorkerStartupRunner.ORDERED;
    }
}

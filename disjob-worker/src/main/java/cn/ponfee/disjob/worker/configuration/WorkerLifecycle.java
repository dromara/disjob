/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.worker.configuration;

import cn.ponfee.disjob.core.base.HttpProperties;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.SupervisorService;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.dispatch.TaskReceiver;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.worker.WorkerStartup;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.lang.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Worker lifecycle
 *
 * @author Ponfee
 */
public class WorkerLifecycle implements SmartLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(WorkerLifecycle.class);

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final WorkerStartup workerStartup;

    public WorkerLifecycle(Worker currentWorker,
                           WorkerProperties workerProperties,
                           RetryProperties retryProperties,
                           HttpProperties httpProperties,
                           WorkerRegistry workerRegistry,
                           TaskReceiver taskReceiver,
                           // if the current server also is a supervisor -> cn.ponfee.disjob.supervisor.provider.SupervisorServiceProvider
                           @Nullable SupervisorService supervisorService,
                           @Nullable ObjectMapper objectMapper) {
        this.workerStartup = WorkerStartup.builder()
            .currentWorker(currentWorker)
            .workerProperties(workerProperties)
            .retryProperties(retryProperties)
            .httpProperties(httpProperties)
            .workerRegistry(workerRegistry)
            .taskReceiver(taskReceiver)
            .supervisorService(supervisorService)
            .objectMapper(objectMapper)
            .build();
    }

    @Override
    public boolean isRunning() {
        return started.get();
    }

    @Override
    public void start() {
        if (!started.compareAndSet(false, true)) {
            LOG.error("Disjob worker lifecycle already stated!");
        }

        LOG.info("Disjob worker launch begin...");
        workerStartup.start();
        LOG.info("Disjob worker launch end.");
    }

    @Override
    public void stop(Runnable callback) {
        if (!started.compareAndSet(true, false)) {
            LOG.error("Disjob worker lifecycle already stopped!");
        }

        LOG.info("Disjob worker stop begin...");
        workerStartup.stop();
        LOG.info("Disjob worker stop end.");

        callback.run();
    }

    @Override
    public void stop() {
        stop(() -> {});
    }

    @Override
    public int getPhase() {
        return DEFAULT_PHASE - 1;
    }

}

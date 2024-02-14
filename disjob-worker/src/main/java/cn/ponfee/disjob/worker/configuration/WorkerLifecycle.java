/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.worker.configuration;

import cn.ponfee.disjob.core.base.HttpProperties;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.SupervisorRpcService;
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

    public WorkerLifecycle(Worker.Current currentWorker,
                           WorkerProperties workerProperties,
                           RetryProperties retryProperties,
                           HttpProperties httpProperties,
                           WorkerRegistry workerRegistry,
                           TaskReceiver taskReceiver,
                           // if the current server also is a supervisor -> cn.ponfee.disjob.supervisor.provider.rpc.SupervisorRpcProvider
                           @Nullable SupervisorRpcService supervisorRpcService,
                           @Nullable ObjectMapper objectMapper) {
        this.workerStartup = WorkerStartup.builder()
            .currentWorker(currentWorker)
            .workerProperties(workerProperties)
            .retryProperties(retryProperties)
            .httpProperties(httpProperties)
            .workerRegistry(workerRegistry)
            .taskReceiver(taskReceiver)
            .supervisorRpcService(supervisorRpcService)
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

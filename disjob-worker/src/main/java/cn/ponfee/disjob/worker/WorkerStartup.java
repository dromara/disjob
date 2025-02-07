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

package cn.ponfee.disjob.worker;

import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.base.Startable;
import cn.ponfee.disjob.common.concurrent.TripState;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.SupervisorRpcService;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.dispatch.TaskReceiver;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.registry.rpc.DiscoveryServerRestProxy;
import cn.ponfee.disjob.worker.base.TimingWheelRotator;
import cn.ponfee.disjob.worker.base.WorkerThreadPool;
import cn.ponfee.disjob.worker.configuration.WorkerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Startup worker.
 *
 * @author Ponfee
 */
public class WorkerStartup extends SingletonClassConstraint implements Startable {

    private static final Logger LOG = LoggerFactory.getLogger(WorkerStartup.class);

    private final Worker.Local localWorker;
    private final WorkerThreadPool workerThreadPool;
    private final TimingWheelRotator timingWheelRotator;
    private final TaskReceiver taskReceiver;
    private final WorkerRegistry workerRegistry;
    private final TripState state = TripState.create();

    public WorkerStartup(Worker.Local localWorker,
                         WorkerProperties workerProperties,
                         RetryProperties retryProperties,
                         WorkerRegistry workerRegistry,
                         TaskReceiver taskReceiver,
                         RestTemplate restTemplate,
                         @Nullable SupervisorRpcService supervisorRpcService) {
        Objects.requireNonNull(localWorker, "Local worker cannot null.");
        Objects.requireNonNull(workerProperties, "Worker properties cannot be null.").check();
        Objects.requireNonNull(retryProperties, "Retry properties cannot be null.").check();
        Objects.requireNonNull(workerRegistry, "Server registry cannot null.");
        Objects.requireNonNull(taskReceiver, "Task receiver cannot null.");
        Objects.requireNonNull(restTemplate, "Rest template cannot null.");

        SupervisorRpcService supervisorRpcClient = DiscoveryServerRestProxy.create(
            SupervisorRpcService.class,
            supervisorRpcService,
            workerRegistry,
            restTemplate,
            retryProperties
        );

        this.localWorker = localWorker;
        this.workerThreadPool = new WorkerThreadPool(
            workerProperties.getMaximumPoolSize(),
            workerProperties.getKeepAliveTimeSeconds(),
            supervisorRpcClient
        );
        this.timingWheelRotator = new TimingWheelRotator(
            supervisorRpcClient,
            workerRegistry,
            taskReceiver.getTimingWheel(),
            workerThreadPool,
            workerProperties.getProcessThreadPoolSize()
        );
        this.taskReceiver = taskReceiver;
        this.workerRegistry = workerRegistry;
    }

    @Override
    public void start() {
        if (!state.start()) {
            LOG.warn("Worker already started.");
            return;
        }

        LOG.info("Worker start begin: {}", localWorker);
        workerThreadPool.start();
        timingWheelRotator.start();
        taskReceiver.start();
        ThrowingRunnable.doCaught(workerRegistry::discoverServers);
        workerRegistry.register(localWorker);
        printBanner();
        LOG.info("Worker start end: {}", localWorker);
    }

    @Override
    public void stop() {
        if (!state.stop()) {
            LOG.warn("Worker already stopped.");
            return;
        }

        LOG.info("Worker stop begin: {}", localWorker);
        ThrowingRunnable.doCaught(workerRegistry::close);
        ThrowingRunnable.doCaught(taskReceiver::close);
        ThrowingRunnable.doCaught(timingWheelRotator::close);
        ThrowingRunnable.doCaught(workerThreadPool::close);
        LOG.info("Worker stop end: {}", localWorker);
    }

    public boolean isRunning() {
        return state.isRunning();
    }

    @SuppressWarnings("all")
    private static void printBanner() {
        String bannerWorker = "Disjob worker banner\n\n" +
            "==================================================================================\n" +
            "      ___ _      _       _        __    __           _\n" +
            "     /   (_)___ (_) ___ | |__    / / /\\ \\ \\___  _ __| | _____ _ __\n" +
            "    / /\\ / / __|| |/ _ \\| '_ \\   \\ \\/  \\/ / _ \\| '__| |/ / _ \\ '__|\n" +
            "   / /_//| \\__ \\| | (_) | |_) |   \\  /\\  / (_) | |  |   <  __/ |\n" +
            "  /___,' |_|___// |\\___/|_.__/     \\/  \\/ \\___/|_|  |_|\\_\\___|_|\n" +
            "              |__/\n\n" +
            "  Worker : " + Worker.local() + "\n" +
            "  Version: " + JobConstants.VERSION + "\n" +
            "==================================================================================\n";
        LOG.info(bannerWorker);
    }

}

/*
 * Copyright 2022-2026 Ponfee (http://www.ponfee.cn/)
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
import cn.ponfee.disjob.common.base.TablePrinter;
import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.common.concurrent.TripleState;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.supervisor.SupervisorRpcService;
import cn.ponfee.disjob.core.worker.Worker;
import cn.ponfee.disjob.core.worker.dto.ExecuteTaskParam;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.registry.rpc.DiscoveryUngroupedServerRestProxy;
import cn.ponfee.disjob.worker.base.TimingWheelRotator;
import cn.ponfee.disjob.worker.base.WorkerThreadPool;
import cn.ponfee.disjob.worker.configuration.WorkerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Startup worker.
 *
 * @author Ponfee
 */
@Slf4j
public class WorkerStartup extends SingletonClassConstraint implements Startable {

    private final WorkerProperties workerConf;
    private final Worker.Local localWorker;
    private final WorkerThreadPool workerThreadPool;
    private final TimingWheelRotator timingWheelRotator;
    private final WorkerRegistry workerRegistry;
    private final TripleState state = TripleState.create();

    public WorkerStartup(WorkerProperties workerConf,
                         Worker.Local localWorker,
                         TimingWheel<ExecuteTaskParam> timingWheel,
                         RetryProperties retryConf,
                         WorkerRegistry workerRegistry,
                         RestTemplate restTemplate,
                         SupervisorRpcService supervisorRpcService) {
        Objects.requireNonNull(workerConf, "Worker properties cannot be null.").check();
        Objects.requireNonNull(localWorker, "Local worker cannot be null.");
        Objects.requireNonNull(timingWheel, "Timing wheel cannot be null.");
        Objects.requireNonNull(retryConf, "Retry properties cannot be null.").check();
        Objects.requireNonNull(workerRegistry, "Worker registry cannot be null.");
        Objects.requireNonNull(restTemplate, "Rest template cannot be null.");

        SupervisorRpcService supervisorRpcClient = DiscoveryUngroupedServerRestProxy.create(SupervisorRpcService.class, supervisorRpcService, workerRegistry, restTemplate, retryConf);

        this.workerConf = workerConf;
        this.localWorker = localWorker;
        this.workerThreadPool = new WorkerThreadPool(workerConf.getMaximumPoolSize(), workerConf.getKeepAliveTimeSeconds(), supervisorRpcClient);
        this.timingWheelRotator = new TimingWheelRotator(supervisorRpcClient, workerRegistry, timingWheel, workerThreadPool, workerConf.getProcessThreadPoolSize());
        this.workerRegistry = workerRegistry;
    }

    @Override
    public void start() {
        if (!state.start()) {
            log.warn("Worker start failed, current state: {}", state);
            return;
        }

        log.info("Worker start begin: {}", localWorker);
        workerThreadPool.start();
        timingWheelRotator.start();
        ThrowingRunnable.doCaught(workerRegistry::discoverServers);
        workerRegistry.register(localWorker);
        printBanner(workerConf.isPrintBannerEnabled());
        log.info("Worker start end: {}", localWorker);
    }

    @Override
    public void stop() {
        if (!state.stop()) {
            log.warn("Worker stop failed, current state: {}", state);
            return;
        }

        log.info("Worker stop begin: {}", localWorker);
        ThrowingRunnable.doCaught(workerRegistry::close);
        ThrowingRunnable.doCaught(timingWheelRotator::close);
        ThrowingRunnable.doCaught(workerThreadPool::close);
        log.info("Worker stop end: {}", localWorker);
    }

    public boolean isRunning() {
        return state.isRunning();
    }

    private static void printBanner(boolean enabled) {
        if (!enabled) {
            return;
        }

        List<String> banner = Arrays.asList(
            "",
            "     ___ _      _       _        __    __           _ ",
            "    /   (_)___ (_) ___ | |__    / / /\\ \\ \\___  _ __| | _____ _ __ ",
            "   / /\\ / / __|| |/ _ \\| '_ \\   \\ \\/  \\/ / _ \\| '__| |/ / _ \\ '__| ",
            "  / /_//| \\__ \\| | (_) | |_) |   \\  /\\  / (_) | |  |   <  __/ | ",
            " /___,' |_|___// |\\___/|_.__/     \\/  \\/ \\___/|_|  |_|\\_\\___|_| ",
            "             |__/ ",
            "",
            " Worker : " + Worker.local() + " ",
            " Version: " + JobConstants.DISJOB_VERSION + " ",
            ""
        );
        log.info("Disjob worker banner\n\n{}\n", TablePrinter.HALF.print(null, banner));
    }

}

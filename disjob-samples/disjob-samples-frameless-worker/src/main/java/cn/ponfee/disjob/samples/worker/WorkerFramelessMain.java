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

package cn.ponfee.disjob.samples.worker;

import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.common.concurrent.ShutdownHookManager;
import cn.ponfee.disjob.common.spring.RestTemplateUtils;
import cn.ponfee.disjob.common.spring.SpringUtils;
import cn.ponfee.disjob.common.spring.YamlProperties;
import cn.ponfee.disjob.common.util.ClassUtils;
import cn.ponfee.disjob.common.util.NetUtils;
import cn.ponfee.disjob.common.util.UuidUtils;
import cn.ponfee.disjob.core.base.CoreUtils;
import cn.ponfee.disjob.core.base.HttpProperties;
import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.worker.Worker;
import cn.ponfee.disjob.core.worker.WorkerRpcService;
import cn.ponfee.disjob.core.worker.dto.ExecuteTaskParam;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.registry.zookeeper.ZookeeperWorkerRegistry;
import cn.ponfee.disjob.registry.zookeeper.configuration.ZookeeperRegistryProperties;
import cn.ponfee.disjob.worker.WorkerStartup;
import cn.ponfee.disjob.worker.base.TaskTimingWheel;
import cn.ponfee.disjob.worker.configuration.WorkerProperties;
import cn.ponfee.disjob.worker.provider.WorkerRpcProvider;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Job worker configuration.
 * <p>Note: non web application not supported HttpTaskReceiver
 *
 * @author Ponfee
 */
public class WorkerFramelessMain {

    static {
        // for log4j2 log file name
        System.setProperty("app.name", "frameless-worker");
        JobExecutorMapping.init();
    }

    private static final Logger log = LoggerFactory.getLogger(WorkerFramelessMain.class);

    public static void main(String[] args) throws Exception {
        // 1、load config
        YamlProperties config = loadConfig(args);
        HttpProperties httpProps = config.bind(JobConstants.HTTP_CONFIG_KEY, HttpProperties.class);
        httpProps.check();
        RetryProperties retryProps = config.bind(JobConstants.RETRY_CONFIG_KEY, RetryProperties.class);
        retryProps.check();
        WorkerProperties workerProps = config.bind(JobConstants.WORKER_CONFIG_KEY, WorkerProperties.class);
        workerProps.check();

        // 2、create component
        RestTemplate restTemplate = RestTemplateUtils.create(httpProps.getConnectTimeout(), httpProps.getReadTimeout(), null);
        Worker.Local localWorker = createLocalWorker(config, workerProps);
        TimingWheel<ExecuteTaskParam> timingWheel = new TaskTimingWheel(workerProps.getTimingWheelTickMs(), workerProps.getTimingWheelRingSize());
        WorkerRegistry workerRegistry = createWorkerRegistry(config, restTemplate);

        // 3、create starter
        VertxWebServer vertxWebServer = createVertxWebServer(localWorker, timingWheel, config, workerRegistry);
        WorkerStartup workerStartup = new WorkerStartup(workerProps, localWorker, timingWheel, retryProps, workerRegistry, restTemplate, null);

        // 4、do start
        log.info("Frameless worker starting...");
        start(vertxWebServer, workerStartup);
        log.info("Frameless worker started.");
    }

    // -----------------------------------------------------------------------------------------------private methods

    private static YamlProperties loadConfig(String[] args) throws IOException {
        String path = ArrayUtils.isEmpty(args) ? null : args[0];
        try (InputStream stream = StringUtils.isEmpty(path) ?
            WorkerFramelessMain.class.getResourceAsStream("/worker-conf.yml") :
            Files.newInputStream(Paths.get(path))) {
            return new YamlProperties(stream);
        }
    }

    private static Worker.Local createLocalWorker(YamlProperties config, WorkerProperties workerProps) {
        Object[] args = {
            workerProps.getGroup(),
            UuidUtils.uuid32(),
            CoreUtils.getLocalHost(config.getString(JobConstants.DISJOB_BOUND_SERVER_HOST)),
            Optional.ofNullable(config.getInt(SpringUtils.SPRING_BOOT_SERVER_PORT)).orElseGet(() -> NetUtils.findAvailablePort(10000)),
            workerProps.getWorkerToken(),
            workerProps.getSupervisorToken(),
            workerProps.getSupervisorContextPath()
        };
        return ClassUtils.invoke(Worker.Local.class, "create", args);
    }

    private static WorkerRegistry createWorkerRegistry(YamlProperties config, RestTemplate restTemplate) {
        ZookeeperRegistryProperties registryProps = config.bind(ZookeeperRegistryProperties.KEY_PREFIX, ZookeeperRegistryProperties.class);
        return new ZookeeperWorkerRegistry(registryProps, restTemplate);
    }

    private static VertxWebServer createVertxWebServer(Worker.Local localWorker, TimingWheel<ExecuteTaskParam> timingWheel,
                                                       YamlProperties config, WorkerRegistry workerRegistry) {
        WorkerRpcService workerRpcService = WorkerRpcProvider.create(localWorker, timingWheel, workerRegistry);
        String workerContextPath = config.getString(SpringUtils.SPRING_BOOT_CONTEXT_PATH);
        return new VertxWebServer(localWorker.getPort(), workerContextPath, workerRpcService);
    }

    private static void start(VertxWebServer vertxWebServer, WorkerStartup workerStartup) {
        ShutdownHookManager.addShutdownHook(0, workerStartup::close);
        ShutdownHookManager.addShutdownHook(Integer.MAX_VALUE, vertxWebServer::close);
        vertxWebServer.deploy();
        workerStartup.start();
    }

}

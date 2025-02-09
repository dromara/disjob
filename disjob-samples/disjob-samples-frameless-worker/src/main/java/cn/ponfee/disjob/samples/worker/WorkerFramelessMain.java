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

package cn.ponfee.disjob.samples.worker;

import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.spring.RestTemplateUtils;
import cn.ponfee.disjob.common.spring.SpringUtils;
import cn.ponfee.disjob.common.spring.YamlProperties;
import cn.ponfee.disjob.common.util.ClassUtils;
import cn.ponfee.disjob.common.util.NetUtils;
import cn.ponfee.disjob.common.util.UuidUtils;
import cn.ponfee.disjob.core.base.*;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import cn.ponfee.disjob.dispatch.TaskReceiver;
import cn.ponfee.disjob.dispatch.http.HttpTaskReceiver;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.registry.redis.RedisWorkerRegistry;
import cn.ponfee.disjob.registry.redis.configuration.RedisRegistryProperties;
import cn.ponfee.disjob.samples.worker.redis.AbstractRedisTemplateCreator;
import cn.ponfee.disjob.samples.worker.util.JobExecutorParser;
import cn.ponfee.disjob.samples.worker.vertx.VertxWebServer;
import cn.ponfee.disjob.worker.WorkerStartup;
import cn.ponfee.disjob.worker.base.TaskTimingWheel;
import cn.ponfee.disjob.worker.configuration.WorkerProperties;
import cn.ponfee.disjob.worker.provider.WorkerRpcProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.client.RestTemplate;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static cn.ponfee.disjob.core.base.JobConstants.DISJOB_BOUND_SERVER_HOST;

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
        JobExecutorParser.init();
    }

    private static final Logger LOG = LoggerFactory.getLogger(WorkerFramelessMain.class);

    public static void main(String[] args) throws Exception {
        // 1、load config
        YamlProperties config = loadConfig(args);
        HttpProperties httpProps = config.bind(HttpProperties.KEY_PREFIX, HttpProperties.class);
        httpProps.check();
        RetryProperties retryProps = config.bind(RetryProperties.KEY_PREFIX, RetryProperties.class);
        retryProps.check();
        WorkerProperties workerProps = config.bind(WorkerProperties.KEY_PREFIX, WorkerProperties.class);
        workerProps.check();
        RestTemplate restTemplate = RestTemplateUtils.create(httpProps.getConnectTimeout(), httpProps.getReadTimeout(), null);

        // 2、create component
        Worker.Local localWorker = createLocalWorker(config, workerProps);
        TaskReceiver taskReceiver = createTaskReceiver(workerProps, localWorker);
        WorkerRegistry workerRegistry = createWorkerRegistry(config, restTemplate);
        VertxWebServer vertxWebServer = createVertxWebServer(config, localWorker, taskReceiver, workerRegistry);
        WorkerStartup workerStartup = new WorkerStartup(localWorker, workerProps, retryProps, workerRegistry, taskReceiver, restTemplate, null);

        // 3、do start
        LOG.info("Frameless worker starting...");
        start(vertxWebServer, workerStartup);
        LOG.info("Frameless worker started.");
    }

    // -----------------------------------------------------------------------------------------------private methods

    private static YamlProperties loadConfig(String[] args) throws IOException {
        String path = Optional.ofNullable(args).filter(e -> e.length > 0).map(e -> e[0]).orElse("");
        try (InputStream stream = path.isEmpty() ? WorkerFramelessMain.class.getResourceAsStream("/worker-conf.yml") : new FileInputStream(path)) {
            return new YamlProperties(stream);
        }
    }

    private static Worker.Local createLocalWorker(YamlProperties config, WorkerProperties workerProps) throws Exception {
        Object[] args = {
            workerProps.getGroup(),
            UuidUtils.uuid32(),
            CoreUtils.getLocalHost(config.getString(DISJOB_BOUND_SERVER_HOST)),
            Optional.ofNullable(config.getInt(SpringUtils.SPRING_BOOT_SERVER_PORT)).orElseGet(() -> NetUtils.findAvailablePort(10000)),
            workerProps.getWorkerToken(),
            workerProps.getSupervisorToken(),
            workerProps.getSupervisorContextPath()
        };
        return ClassUtils.invoke(Class.forName(Worker.Local.class.getName()), "create", args);
    }

    private static TaskReceiver createTaskReceiver(WorkerProperties props, Worker.Local localWorker) {
        TimingWheel<ExecuteTaskParam> timingWheel = new TaskTimingWheel(props.getTimingWheelTickMs(), props.getTimingWheelRingSize());
        return new HttpTaskReceiver(localWorker, timingWheel);
    }

    private static WorkerRegistry createWorkerRegistry(YamlProperties config, RestTemplate restTemplate) {
        RedisRegistryProperties redisRegistryProps = config.bind(RedisRegistryProperties.KEY_PREFIX, RedisRegistryProperties.class);
        StringRedisTemplate stringRedisTemplate = AbstractRedisTemplateCreator.create(RedisRegistryProperties.KEY_PREFIX, config).getStringRedisTemplate();
        return new RedisWorkerRegistry(redisRegistryProps, restTemplate, stringRedisTemplate);
    }

    private static VertxWebServer createVertxWebServer(YamlProperties config, Worker.Local localWorker, TaskReceiver taskReceiver, WorkerRegistry workerRegistry) {
        WorkerRpcService workerRpcService = WorkerRpcProvider.create(localWorker, workerRegistry);
        String workerContextPath = config.getString(SpringUtils.SPRING_BOOT_CONTEXT_PATH);
        return new VertxWebServer(localWorker.getPort(), workerContextPath, taskReceiver, workerRpcService);
    }

    private static void start(VertxWebServer vertxWebServer, WorkerStartup workerStartup) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ThrowingRunnable.doCaught(workerStartup::close);
            ThrowingRunnable.doCaught(vertxWebServer::close);
        }));
        vertxWebServer.deploy();
        workerStartup.start();
    }

}

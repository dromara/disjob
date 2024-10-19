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

import cn.ponfee.disjob.common.base.LazyLoader;
import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.common.exception.Throwables;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.spring.RestTemplateUtils;
import cn.ponfee.disjob.common.spring.SpringUtils;
import cn.ponfee.disjob.common.spring.YamlProperties;
import cn.ponfee.disjob.common.util.ClassUtils;
import cn.ponfee.disjob.common.util.NetUtils;
import cn.ponfee.disjob.common.util.UuidUtils;
import cn.ponfee.disjob.core.base.CoreUtils;
import cn.ponfee.disjob.core.base.HttpProperties;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.Worker;
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
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.client.RestTemplate;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static cn.ponfee.disjob.core.base.JobConstants.DISJOB_BOUND_SERVER_HOST;
import static cn.ponfee.disjob.core.base.JobConstants.DISJOB_KEY_PREFIX;
import static java.nio.charset.StandardCharsets.UTF_8;

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
        printBanner();
        JobExecutorParser.init();
    }

    private static final Logger LOG = LoggerFactory.getLogger(WorkerFramelessMain.class);

    public static void main(String[] args) throws Exception {
        YamlProperties config = loadConfig(args);
        HttpProperties httpProps = config.extract(HttpProperties.class, HttpProperties.KEY_PREFIX);
        httpProps.check();
        WorkerProperties workerProps = config.extract(WorkerProperties.class, WorkerProperties.KEY_PREFIX);
        workerProps.check();
        LazyLoader<StringRedisTemplate> srtLoader = LazyLoader.of(() -> AbstractRedisTemplateCreator.create(DISJOB_KEY_PREFIX + ".redis", config).getStringRedisTemplate());
        Worker.Local localWorker = createLocalWorker(config, workerProps);
        TimingWheel<ExecuteTaskParam> timingWheel = new TaskTimingWheel(workerProps.getTimingWheelTickMs(), workerProps.getTimingWheelRingSize());



        // --------------------- create registry(select redis or consul) --------------------- //
        WorkerRegistry workerRegistry;
        {
            // 1）redis registry
            workerRegistry = new RedisWorkerRegistry(srtLoader.get(), config.extract(RedisRegistryProperties.class, RedisRegistryProperties.KEY_PREFIX));

            // 2）consul registry
            //workerRegistry = new ConsulWorkerRegistry(config.extract(ConsulRegistryProperties.class, ConsulRegistryProperties.KEY_PREFIX));
        }
        // --------------------- create registry(select redis or consul) --------------------- //


        // --------------------- create receiver(select redis or http) --------------------- //
        TaskReceiver actualTaskReceiver, paramTaskReceiver;
        {
            // 1）redis receiver
            /*
            actualTaskReceiver = new RedisTaskReceiver(localWorker, timingWheel, srtLoader.get()) {
                @Override
                public boolean receive(ExecuteTaskParam param) {
                    JobExecutorParser.parse(param, "jobExecutor");
                    return super.receive(param);
                }
            };
            // Redis task receiver cannot support http
            paramTaskReceiver = null;
            */

            // 2）http receiver
            paramTaskReceiver = actualTaskReceiver = new HttpTaskReceiver(localWorker, timingWheel);
        }
        // --------------------- create receiver(select redis or http) --------------------- //



        // `verify/split/metrics/configure` 接口还是要走http
        String workerContextPath = config.getString(SpringUtils.SPRING_BOOT_CONTEXT_PATH);
        VertxWebServer vertxWebServer = new VertxWebServer(localWorker.getPort(), workerContextPath, paramTaskReceiver, new WorkerRpcProvider(localWorker, workerRegistry));
        RetryProperties retryProperties = config.extract(RetryProperties.class, RetryProperties.KEY_PREFIX);
        RestTemplate restTemplate = RestTemplateUtils.create(httpProps.getConnectTimeout(), httpProps.getReadTimeout(), null);
        WorkerStartup workerStartup = new WorkerStartup(localWorker, workerProps, retryProperties, workerRegistry, actualTaskReceiver, restTemplate, null);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> close(workerStartup, vertxWebServer)));

        // do start
        LOG.info("Frameless worker starting...");
        vertxWebServer.deploy();
        workerStartup.start();
        LOG.info("Frameless worker started.");

        //new CountDownLatch(1).await();
    }

    // -----------------------------------------------------------------------------------------------private methods

    private static void close(WorkerStartup workerStartup, VertxWebServer vertxWebServer) {
        ThrowingRunnable.doCaught(workerStartup::close);
        ThrowingRunnable.doCaught(vertxWebServer::close);
    }

    private static void printBanner() {
        ClassLoader classLoader = WorkerStartup.class.getClassLoader();
        String banner = Throwables.ThrowingSupplier.doChecked(() -> IOUtils.resourceToString("banner.txt", UTF_8, classLoader));
        System.out.println(banner);
    }

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

}

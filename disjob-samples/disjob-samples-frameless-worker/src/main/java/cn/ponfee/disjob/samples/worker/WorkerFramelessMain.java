/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.samples.worker;

import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.spring.YamlProperties;
import cn.ponfee.disjob.common.util.ClassUtils;
import cn.ponfee.disjob.common.util.NetUtils;
import cn.ponfee.disjob.common.util.UuidUtils;
import cn.ponfee.disjob.core.base.*;
import cn.ponfee.disjob.core.util.JobUtils;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import cn.ponfee.disjob.dispatch.TaskReceiver;
import cn.ponfee.disjob.dispatch.http.HttpTaskReceiver;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.registry.redis.RedisWorkerRegistry;
import cn.ponfee.disjob.registry.redis.configuration.RedisRegistryProperties;
import cn.ponfee.disjob.samples.worker.redis.AbstractRedisTemplateCreator;
import cn.ponfee.disjob.samples.worker.util.JobHandlerParser;
import cn.ponfee.disjob.samples.worker.vertx.VertxWebServer;
import cn.ponfee.disjob.worker.WorkerStartup;
import cn.ponfee.disjob.worker.base.TaskTimingWheel;
import cn.ponfee.disjob.worker.configuration.WorkerProperties;
import cn.ponfee.disjob.worker.provider.WorkerCoreRpcProvider;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import static cn.ponfee.disjob.core.base.JobConstants.*;

/**
 * Job worker configuration.
 * <p>Note: non web application not supported HttpTaskReceiver
 *
 * @author Ponfee
 */
public class WorkerFramelessMain {

    static {
        // for log4j log file dir
        System.setProperty("app.name", "frameless-worker");
        JobHandlerParser.init();
    }

    private static final Logger LOG = LoggerFactory.getLogger(WorkerFramelessMain.class);

    private static StringRedisTemplate stringRedisTemplate = null;

    public static void main(String[] args) throws Exception {
        printBanner();

        YamlProperties props;
        try (InputStream inputStream = loadConfigStream(args)) {
            props = new YamlProperties(inputStream);
        }

        WorkerProperties workerProperties = props.extract(WorkerProperties.class, WORKER_KEY_PREFIX + ".");
        int port = Optional.ofNullable(props.getInt("server.port")).orElse(NetUtils.findAvailablePort(10000));

        String group = props.getString(WORKER_KEY_PREFIX + ".group");
        Assert.hasText(group, "Worker group name cannot empty.");
        String boundHost = JobUtils.getLocalHost(props.getString(DISJOB_BOUND_SERVER_HOST));

        Object[] array = {group, UuidUtils.uuid32(), boundHost, port, workerProperties.getWorkerToken(), workerProperties.getSupervisorToken()};
        Worker.Current currentWorker = ClassUtils.invoke(Class.forName(Worker.Current.class.getName()), "create", array);

        TimingWheel<ExecuteTaskParam> timingWheel = new TaskTimingWheel(
            props.getLong(WORKER_KEY_PREFIX + ".timing-wheel-tick-ms", 100),
            props.getInt(WORKER_KEY_PREFIX + ".timing-wheel-ring-size", 60)
        );




        // --------------------- create registry(select redis or consul) --------------------- //
        WorkerRegistry workerRegistry;
        {
            // redis registry
            workerRegistry = createRedisWorkerRegistry(JobConstants.DISJOB_REGISTRY_KEY_PREFIX + ".redis", props);

            // consul registry
            //workerRegistry = createConsulWorkerRegistry(JobConstants.DISJOB_REGISTRY_KEY_PREFIX + ".consul", props);
        }
        // --------------------- create registry(select redis or consul) --------------------- //




        // --------------------- create registry(select redis or http) --------------------- //
        TaskReceiver taskReceiver;
        VertxWebServer vertxWebServer;
        WorkerCoreRpcService workerCoreRpcProvider = new WorkerCoreRpcProvider(currentWorker);
        {
            // redis dispatching
            //taskReceiver = new RedisTaskReceiver(currentWorker, timingWheel, stringRedisTemplate) {
            //    @Override
            //    public boolean receive(ExecuteTaskParam param) {
            //        JobHandlerParser.parse(param, "jobHandler");
            //        return super.receive(param);
            //    }
            //};
            //vertxWebServer = new VertxWebServer(port, null, workerCoreRpcProvider);

            // http dispatching
            taskReceiver = new HttpTaskReceiver(currentWorker, timingWheel);
            vertxWebServer = new VertxWebServer(port, taskReceiver, workerCoreRpcProvider);
        }
        // --------------------- create registry(select redis or http) --------------------- //




        WorkerStartup workerStartup = WorkerStartup.builder()
            .currentWorker(currentWorker)
            .workerProperties(workerProperties)
            .retryProperties(props.extract(RetryProperties.class, RETRY_KEY_PREFIX + "."))
            .httpProperties(props.extract(HttpProperties.class, HTTP_KEY_PREFIX + "."))
            .taskReceiver(taskReceiver)
            .workerRegistry(workerRegistry)
            .build();

        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                ThrowingRunnable.doCaught(workerStartup::close);
                ThrowingRunnable.doCaught(vertxWebServer::close);
            }));

            vertxWebServer.deploy();
            workerStartup.start();

            new CountDownLatch(1).await();
        } catch (InterruptedException e) {
            LOG.error("Sleep interrupted.", e);
            Thread.currentThread().interrupt();
        } finally {
            workerStartup.close();
        }
    }

    private static InputStream loadConfigStream(String[] args) throws FileNotFoundException {
        String filePath;
        if (StringUtils.isEmpty(filePath = Collects.get(args, 0))) {
            return Thread.currentThread().getContextClassLoader().getResourceAsStream("worker-conf.yml");
        } else {
            return new FileInputStream(filePath);
        }
    }

    private static void printBanner() throws IOException {
        String banner = IOUtils.resourceToString(
            "banner.txt", StandardCharsets.UTF_8, WorkerStartup.class.getClassLoader()
        );
        System.out.println(banner);
    }

    private static WorkerRegistry createRedisWorkerRegistry(String keyPrefix, YamlProperties props) {
        RedisRegistryProperties config = new RedisRegistryProperties();
        config.setNamespace(props.getString(keyPrefix + ".namespace"));
        config.setSessionTimeoutMs(props.getLong(keyPrefix + ".session-timeout-ms", 30000));
        return new RedisWorkerRegistry(stringRedisTemplate(props), config);
    }

    /*
    private static WorkerRegistry createConsulWorkerRegistry(String keyPrefix, YamlProperties props) {
        ConsulRegistryProperties config = new ConsulRegistryProperties();
        config.setNamespace(props.getString(keyPrefix + ".namespace"));
        config.setHost(props.getString(keyPrefix + ".host", "localhost"));
        config.setPort(props.getInt(keyPrefix + ".port", 8500));
        config.setToken(props.getString(keyPrefix + ".token"));
        return new ConsulWorkerRegistry(config);
    }
    */

    private synchronized static StringRedisTemplate stringRedisTemplate(YamlProperties props) {
        if (stringRedisTemplate == null) {
            String keyPrefix = DISJOB_KEY_PREFIX + ".redis.";
            stringRedisTemplate = AbstractRedisTemplateCreator.create(keyPrefix, props, null).getStringRedisTemplate();
        }
        return stringRedisTemplate;
    }

}

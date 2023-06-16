/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.samples.worker;

import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.spring.YamlProperties;
import cn.ponfee.disjob.common.util.*;
import cn.ponfee.disjob.core.base.*;
import cn.ponfee.disjob.core.param.ExecuteTaskParam;
import cn.ponfee.disjob.core.util.JobUtils;
import cn.ponfee.disjob.dispatch.TaskReceiver;
import cn.ponfee.disjob.dispatch.http.HttpTaskReceiver;
import cn.ponfee.disjob.registry.DiscoveryRestProxy;
import cn.ponfee.disjob.registry.DiscoveryRestTemplate;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.registry.redis.RedisWorkerRegistry;
import cn.ponfee.disjob.registry.redis.configuration.RedisRegistryProperties;
import cn.ponfee.disjob.samples.common.util.SampleConstants;
import cn.ponfee.disjob.samples.worker.redis.AbstractRedisTemplateCreator;
import cn.ponfee.disjob.samples.worker.vertx.VertxWebServer;
import cn.ponfee.disjob.worker.WorkerStartup;
import cn.ponfee.disjob.worker.base.TaskTimingWheel;
import cn.ponfee.disjob.worker.configuration.WorkerProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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

import static cn.ponfee.disjob.core.base.JobConstants.DISJOB_BOUND_SERVER_HOST;
import static cn.ponfee.disjob.core.base.JobConstants.WORKER_KEY_PREFIX;

/**
 * Job worker configuration.
 * <p>Note: non web application not supported HttpTaskReceiver
 *
 * @author Ponfee
 */
public class Main {

    static {
        // for log4j log file dir
        System.setProperty(SampleConstants.APP_NAME, "frameless-worker");
    }

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static StringRedisTemplate stringRedisTemplate = null;

    public static void main(String[] args) throws Exception {
        printBanner();

        YamlProperties props;
        try (InputStream inputStream = loadConfigStream(args)) {
            props = new YamlProperties(inputStream);
        }

        int port = Optional.ofNullable(props.getInt("server.port")).orElse(NetUtils.findAvailablePort(10000));

        String group = props.getString(WORKER_KEY_PREFIX + ".group");
        Assert.hasText(group, "Worker group name cannot empty.");
        String boundHost = JobUtils.getLocalHost(props.getString(DISJOB_BOUND_SERVER_HOST));
        Worker currentWorker = new Worker(group, ObjectUtils.uuid32(), boundHost, port);
        // inject current worker
        ClassUtils.invoke(Class.forName(Worker.class.getName() + "$Current"), "set", new Object[]{currentWorker});

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
        {
            // redis dispatching
            //taskReceiver = new RedisTaskReceiver(currentWorker, timingWheel, stringRedisTemplate);
            //vertxWebServer = new VertxWebServer(port, null);

            // http dispatching
            taskReceiver = new HttpTaskReceiver(timingWheel);
            vertxWebServer = new VertxWebServer(port, taskReceiver);
        }
        // --------------------- create registry(select redis or http) --------------------- //



        int retryMaxCount = props.getInt(JobConstants.RETRY_KEY_PREFIX + ".max-count", 3);
        int retryBackoffPeriod = props.getInt(JobConstants.RETRY_KEY_PREFIX + ".backoff-period", 5000);
        DiscoveryRestTemplate<Supervisor> discoveryRestTemplate = DiscoveryRestTemplate.<Supervisor>builder()
            .httpConnectTimeout(props.getInt(JobConstants.HTTP_KEY_PREFIX + ".connect-timeout", 2000))
            .httpReadTimeout(props.getInt(JobConstants.HTTP_KEY_PREFIX + ".read-timeout", 5000))
            .retryMaxCount(retryMaxCount)
            .retryBackoffPeriod(retryBackoffPeriod)
            .objectMapper(Jsons.createObjectMapper(JsonInclude.Include.NON_NULL))
            .discoveryServer(workerRegistry)
            .build();
        SupervisorService SupervisorServiceClient = DiscoveryRestProxy.create(false, SupervisorService.class, discoveryRestTemplate);

        WorkerProperties workerConfig = props.extract(WorkerProperties.class, WORKER_KEY_PREFIX + ".");
        WorkerStartup workerStartup = WorkerStartup.builder()
            .currentWorker(currentWorker)
            .retryProperties(RetryProperties.of(retryMaxCount, retryBackoffPeriod))
            .workerConfig(workerConfig)
            .supervisorServiceClient(SupervisorServiceClient)
            .taskReceiver(taskReceiver)
            .workerRegistry(workerRegistry)
            .build();

        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                ThrowingRunnable.caught(workerStartup::close);
                ThrowingRunnable.caught(vertxWebServer::close);
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
        config.setRegistryPeriodMs(props.getLong(keyPrefix + ".registry-period-ms", 3000));
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
            stringRedisTemplate = AbstractRedisTemplateCreator.create("redis.", props).getStringRedisTemplate();
        }
        return stringRedisTemplate;
    }

}

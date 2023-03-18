/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.samples.worker;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.common.base.exception.CheckedThrowing;
import cn.ponfee.scheduler.common.base.exception.Throwables;
import cn.ponfee.scheduler.common.spring.YamlProperties;
import cn.ponfee.scheduler.common.util.*;
import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.SupervisorService;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.core.util.JobUtils;
import cn.ponfee.scheduler.dispatch.TaskReceiver;
import cn.ponfee.scheduler.dispatch.http.HttpTaskReceiver;
import cn.ponfee.scheduler.registry.DiscoveryRestProxy;
import cn.ponfee.scheduler.registry.DiscoveryRestTemplate;
import cn.ponfee.scheduler.registry.WorkerRegistry;
import cn.ponfee.scheduler.registry.redis.RedisWorkerRegistry;
import cn.ponfee.scheduler.registry.redis.configuration.RedisRegistryProperties;
import cn.ponfee.scheduler.samples.common.util.SampleConstants;
import cn.ponfee.scheduler.samples.worker.redis.AbstractRedisTemplateCreator;
import cn.ponfee.scheduler.samples.worker.vertx.VertxWebServer;
import cn.ponfee.scheduler.worker.WorkerStartup;
import cn.ponfee.scheduler.worker.base.TaskTimingWheel;
import cn.ponfee.scheduler.worker.configuration.WorkerProperties;
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

import static cn.ponfee.scheduler.core.base.JobConstants.SCHEDULER_BOUND_SERVER_HOST;
import static cn.ponfee.scheduler.core.base.JobConstants.WORKER_KEY_PREFIX;

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
        String boundHost = JobUtils.getLocalHost(props.getString(SCHEDULER_BOUND_SERVER_HOST));
        Worker currentWorker = new Worker(group, ObjectUtils.uuid32(), boundHost, port);
        // inject current worker
        ClassUtils.invoke(Class.forName(Worker.class.getName() + "$Current"), "set", new Object[]{currentWorker});

        TimingWheel<ExecuteParam> timingWheel = new TaskTimingWheel(
            props.getLong(WORKER_KEY_PREFIX + ".timing-wheel-tick-ms", 100),
            props.getInt(WORKER_KEY_PREFIX + ".timing-wheel-ring-size", 60)
        );




        // --------------------- create registry(select redis or consul) --------------------- //
        WorkerRegistry workerRegistry;
        {
            // redis registry
            workerRegistry = createRedisWorkerRegistry(JobConstants.SCHEDULER_REGISTRY_KEY_PREFIX + ".redis", props);

            // consul registry
            //workerRegistry = createConsulWorkerRegistry(JobConstants.SCHEDULER_REGISTRY_KEY_PREFIX + ".consul", props);
        }
        // --------------------- create registry(select redis or consul) --------------------- //




        // --------------------- create registry(select redis or http) --------------------- //
        TaskReceiver taskReceiver;
        VertxWebServer vertxWebServer;
        {
            // redis dispatching
            //taskReceiver = new RedisTaskReceiver(currentWorker, timingWheel, stringRedisTemplate);
            //vertxWebServer = new VertxWebServer(props.getRequiredInt("server.port"), null);

            // http dispatching
            taskReceiver = new HttpTaskReceiver(timingWheel);
            vertxWebServer = new VertxWebServer(port, taskReceiver);
        }
        // --------------------- create registry(select redis or http) --------------------- //




        DiscoveryRestTemplate<Supervisor> discoveryRestTemplate = DiscoveryRestTemplate.<Supervisor>builder()
            .connectTimeout(props.getInt(JobConstants.SCHEDULER_KEY_PREFIX + ".http.connect-timeout", 2000))
            .readTimeout(props.getInt(JobConstants.SCHEDULER_KEY_PREFIX + ".http.read-timeout", 5000))
            .maxRetryTimes(props.getInt(JobConstants.SCHEDULER_KEY_PREFIX + ".http.max-retry-times", 3))
            .objectMapper(Jsons.createObjectMapper(JsonInclude.Include.NON_NULL))
            .discoveryServer(workerRegistry)
            .build();
        SupervisorService SupervisorServiceClient = DiscoveryRestProxy.create(false, SupervisorService.class, discoveryRestTemplate);

        WorkerProperties workerConfig = props.extract(WorkerProperties.class, WORKER_KEY_PREFIX + ".");
        WorkerStartup workerStartup = WorkerStartup.builder()
            .currentWorker(currentWorker)
            .workerConfig(workerConfig)
            .supervisorServiceClient(SupervisorServiceClient)
            .taskReceiver(taskReceiver)
            .workerRegistry(workerRegistry)
            .build();

        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                Throwables.caught(workerStartup::close);
                CheckedThrowing.caught(vertxWebServer::close);
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

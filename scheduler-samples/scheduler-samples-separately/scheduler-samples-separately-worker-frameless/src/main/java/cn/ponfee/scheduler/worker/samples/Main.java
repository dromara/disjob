package cn.ponfee.scheduler.worker.samples;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.common.spring.YamlProperties;
import cn.ponfee.scheduler.common.util.*;
import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.SupervisorService;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.dispatch.TaskReceiver;
import cn.ponfee.scheduler.dispatch.redis.RedisTaskReceiver;
import cn.ponfee.scheduler.registry.DiscoveryRestProxy;
import cn.ponfee.scheduler.registry.DiscoveryRestTemplate;
import cn.ponfee.scheduler.registry.WorkerRegistry;
import cn.ponfee.scheduler.registry.consul.ConsulWorkerRegistry;
import cn.ponfee.scheduler.registry.redis.RedisWorkerRegistry;
import cn.ponfee.scheduler.worker.WorkerStartup;
import cn.ponfee.scheduler.worker.base.TaskTimingWheel;
import cn.ponfee.scheduler.worker.samples.redis.SentinelRedisTemplateCreator;
import cn.ponfee.scheduler.worker.samples.redis.StandaloneRedisTemplateCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static cn.ponfee.scheduler.core.base.JobConstants.WORKER_KEY_PREFIX;

/**
 * Job worker configuration.
 * <p>Note: non web application not supported HttpTaskReceiver
 *
 * @author Ponfee
 */
@Component
public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        YamlProperties yamlProperties;
        try (InputStream inputStream = loadConfigStream(args)) {
            yamlProperties = new YamlProperties(inputStream);
        }

        String group = yamlProperties.getString(WORKER_KEY_PREFIX + ".group");
        Assert.hasText(group, "Worker group name cannot empty.");
        Worker currentWorker = new Worker(group, ObjectUtils.uuid32(), Networks.getHostIp(), 0);
        // inject current worker
        ClassUtils.invoke(Class.forName(Worker.class.getName() + "$Current"), "set", new Object[]{currentWorker});

        StringRedisTemplate stringRedisTemplate =

            // Standalone
            /*StandaloneRedisTemplateCreator.builder()
                .host(yamlProperties.getRequiredString("redis.host"))
                .port(yamlProperties.getRequiredInt("redis.port"))*/

                // Sentinel
                SentinelRedisTemplateCreator.builder()
                .sentinelMaster(yamlProperties.getRequiredString("redis.sentinel.master"))
                .sentinelNodes(yamlProperties.getRequiredString("redis.sentinel.nodes"))

                .database(yamlProperties.getInt("redis.database", 0))
                .username(yamlProperties.getString("redis.username"))
                .password(yamlProperties.getString("redis.password"))
                .connectTimeout(yamlProperties.getInt("redis.connect-timeout", 1000))
                .timeout(yamlProperties.getInt("redis.timeout", 2000))
                .maxActive(yamlProperties.getInt("redis.lettuce.pool.max-active", 50))
                .maxIdle(yamlProperties.getInt("redis.lettuce.pool.max-idle", 8))
                .minIdle(yamlProperties.getInt("redis.lettuce.pool.min-idle", 0))
                .maxWait(yamlProperties.getInt("redis.lettuce.pool.max-wait", 2000))
                .shutdownTimeout(yamlProperties.getInt("redis.lettuce.shutdown-timeout", 2000))
                .build()
                .create()
                .getStringRedisTemplate();



        //WorkerRegistry workerRegistry = new RedisWorkerRegistry(stringRedisTemplate);
        WorkerRegistry workerRegistry = new ConsulWorkerRegistry("127.0.0.1", 8500, null);

        DiscoveryRestTemplate<Supervisor> discoveryRestTemplate = DiscoveryRestTemplate.<Supervisor>builder()
            .connectTimeout(yamlProperties.getInt(JobConstants.SCHEDULER_KEY_PREFIX + ".http.connect-timeout", 2000))
            .readTimeout(yamlProperties.getInt(JobConstants.SCHEDULER_KEY_PREFIX + ".http.read-timeout", 5000))
            .maxRetryTimes(yamlProperties.getInt(JobConstants.SCHEDULER_KEY_PREFIX + ".http.max-retry-times", 3))
            .objectMapper(Jsons.createObjectMapper(JsonInclude.Include.NON_NULL))
            .discoveryServer(workerRegistry)
            .build();
        SupervisorService supervisorServiceClient = DiscoveryRestProxy.create(SupervisorService.class, discoveryRestTemplate);

        TimingWheel<ExecuteParam> timingWheel = new TaskTimingWheel();
        // 不支持HttpTaskReceiver，请使用scheduler-samples-separately-worker-springboot模块来支持
        TaskReceiver taskReceiver = new RedisTaskReceiver(currentWorker, timingWheel, stringRedisTemplate);

        WorkerStartup workerStartup = WorkerStartup.builder()
            .currentWorker(currentWorker)
            .maximumPoolSize(yamlProperties.getInt(WORKER_KEY_PREFIX + ".maximum-pool-size"))
            .keepAliveTimeSeconds(yamlProperties.getInt(WORKER_KEY_PREFIX + ".keep-alive-time-seconds"))
            .supervisorService(supervisorServiceClient)
            .taskReceiver(taskReceiver)
            .workerRegistry(workerRegistry)
            .build();

        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> workerStartup.close()));
            workerStartup.start();
            while (true) {
                TimeUnit.DAYS.sleep(1);
            }
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
}

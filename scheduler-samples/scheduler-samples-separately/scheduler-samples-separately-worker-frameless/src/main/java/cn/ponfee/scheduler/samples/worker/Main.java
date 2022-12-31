package cn.ponfee.scheduler.samples.worker;

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
import cn.ponfee.scheduler.samples.worker.redis.AbstractRedisTemplateCreator;
import cn.ponfee.scheduler.worker.WorkerStartup;
import cn.ponfee.scheduler.worker.base.TaskTimingWheel;
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
import java.util.concurrent.CountDownLatch;

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
        YamlProperties props;
        try (InputStream inputStream = loadConfigStream(args)) {
            props = new YamlProperties(inputStream);
        }

        String group = props.getString(WORKER_KEY_PREFIX + ".group");
        Assert.hasText(group, "Worker group name cannot empty.");
        Worker currentWorker = new Worker(group, ObjectUtils.uuid32(), Networks.getHostIp(), 0);
        // inject current worker
        ClassUtils.invoke(Class.forName(Worker.class.getName() + "$Current"), "set", new Object[]{currentWorker});

        StringRedisTemplate stringRedisTemplate = AbstractRedisTemplateCreator.create("redis.", props).getStringRedisTemplate();

        String namespace = props.getString(JobConstants.SCHEDULER_NAMESPACE);
        WorkerRegistry workerRegistry = new RedisWorkerRegistry(namespace, stringRedisTemplate);
        //WorkerRegistry workerRegistry = new ConsulWorkerRegistry(namespace, "127.0.0.1", 8500, null);

        DiscoveryRestTemplate<Supervisor> discoveryRestTemplate = DiscoveryRestTemplate.<Supervisor>builder()
            .connectTimeout(props.getInt(JobConstants.SCHEDULER_KEY_PREFIX + ".http.connect-timeout", 2000))
            .readTimeout(props.getInt(JobConstants.SCHEDULER_KEY_PREFIX + ".http.read-timeout", 5000))
            .maxRetryTimes(props.getInt(JobConstants.SCHEDULER_KEY_PREFIX + ".http.max-retry-times", 3))
            .objectMapper(Jsons.createObjectMapper(JsonInclude.Include.NON_NULL))
            .discoveryServer(workerRegistry)
            .build();
        SupervisorService supervisorClient = DiscoveryRestProxy.create(SupervisorService.class, discoveryRestTemplate);

        TimingWheel<ExecuteParam> timingWheel = new TaskTimingWheel();
        // 此为非web应用，不支持HttpTaskReceiver（注：scheduler-samples-separately-worker-springboot应用可以支持HttpTaskReceiver）
        TaskReceiver taskReceiver = new RedisTaskReceiver(currentWorker, timingWheel, stringRedisTemplate);

        WorkerStartup workerStartup = WorkerStartup.builder()
            .currentWorker(currentWorker)
            .maximumPoolSize(props.getInt(WORKER_KEY_PREFIX + ".maximum-pool-size"))
            .keepAliveTimeSeconds(props.getInt(WORKER_KEY_PREFIX + ".keep-alive-time-seconds"))
            .supervisorClient(supervisorClient)
            .taskReceiver(taskReceiver)
            .workerRegistry(workerRegistry)
            .build();

        try {
            Runtime.getRuntime().addShutdownHook(new Thread(workerStartup::close));
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

}

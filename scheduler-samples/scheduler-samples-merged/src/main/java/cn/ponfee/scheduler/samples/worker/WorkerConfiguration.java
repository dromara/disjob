package cn.ponfee.scheduler.samples.worker;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.common.util.ClassUtils;
import cn.ponfee.scheduler.common.util.Networks;
import cn.ponfee.scheduler.common.util.ObjectUtils;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.dispatch.TaskReceiver;
import cn.ponfee.scheduler.dispatch.redis.RedisTaskReceiver;
import cn.ponfee.scheduler.registry.ServerRegistry;
import cn.ponfee.scheduler.registry.redis.RedisWorkerRegistry;
import cn.ponfee.scheduler.supervisor.manager.JobManager;
import cn.ponfee.scheduler.worker.WorkerStartup;
import cn.ponfee.scheduler.worker.base.TaskTimingWheel;
import cn.ponfee.scheduler.worker.client.HttpWorkerClient;
import cn.ponfee.scheduler.worker.client.WorkerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;

import static cn.ponfee.scheduler.worker.base.WorkerConstants.DISTRIBUTED_SCHEDULER_WORKER;

/**
 * Job worker configuration.
 *
 * @author Ponfee
 */
@Configuration
public class WorkerConfiguration implements ApplicationRunner, DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(WorkerConfiguration.class);

    private final WorkerStartup workerStartup;

    public WorkerConfiguration(@Value("${server.port}") int port,
                               @Value("${" + DISTRIBUTED_SCHEDULER_WORKER + ".group:default}") String group,
                               @Value("${" + DISTRIBUTED_SCHEDULER_WORKER + ".maximum-pool-size:20}") int maximumPoolSize,
                               @Value("${" + DISTRIBUTED_SCHEDULER_WORKER + ".keep-alive-time-seconds:300}") int keepAliveTimeSeconds,
                               StringRedisTemplate redisTemplate,
                               JobManager jobManager) throws ClassNotFoundException {
        Assert.hasText(group, "Worker group name cannot empty.");
        Assert.isTrue(port > 0, "Worker port must be greater zero.");
        Worker currentWorker = new Worker(group, ObjectUtils.uuid32(), Networks.getHostIp(), port);
        // inject current worker
        ClassUtils.invoke(Class.forName(Worker.class.getName() + "$Current"), "set", new Object[]{currentWorker});

        TimingWheel<ExecuteParam> timingWheel = new TaskTimingWheel();
        ServerRegistry<Worker, Supervisor> workerRegistry = new RedisWorkerRegistry(redisTemplate);
        WorkerClient workerClient = new HttpWorkerClient(workerRegistry);
        //WorkerClient workerClient = new DBWorkerClient(jobManager);
        TaskReceiver taskReceiver = new RedisTaskReceiver(currentWorker, timingWheel, redisTemplate);

        this.workerStartup = WorkerStartup.builder()
                                          .maximumPoolSize(maximumPoolSize)
                                          .keepAliveTimeSeconds(keepAliveTimeSeconds)
                                          .workerClient(workerClient)
                                          .currentWorker(currentWorker)
                                          .taskReceiver(taskReceiver)
                                          .workerRegistry(workerRegistry)
                                          .build();
    }

    @Override
    public void run(ApplicationArguments args) {
        workerStartup.start();
    }

    @Override
    public void destroy() {
        LOG.info("Job worker destroy start...");
        workerStartup.close();
        LOG.info("Job worker destroy end.");
    }

}

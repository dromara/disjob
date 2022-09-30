package cn.ponfee.scheduler.supervisor.samples.config;

import cn.ponfee.scheduler.common.base.IdGenerator;
import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.common.lock.DoInLocked;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.dispatch.TaskDispatcher;
import cn.ponfee.scheduler.dispatch.http.HttpTaskDispatcher;
import cn.ponfee.scheduler.registry.Discovery;
import cn.ponfee.scheduler.registry.DiscoveryRestTemplate;
import cn.ponfee.scheduler.registry.ServerRegistry;
import cn.ponfee.scheduler.registry.redis.RedisSupervisorRegistry;
import cn.ponfee.scheduler.supervisor.SupervisorStartup;
import cn.ponfee.scheduler.supervisor.manager.JobManager;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;

import static cn.ponfee.scheduler.supervisor.base.SupervisorConstants.*;

/**
 * Job supervisor configuration.
 *
 * @author Ponfee
 */
//@Configuration
public class HttpTaskDispatchSupervisorConfiguration {

    /**
     * RedisAutoConfiguration has auto-configured two redis template objects.
     * <p>RedisTemplate<Object, Object> redisTemplate
     * <p>StringRedisTemplate stringRedisTemplate
     *
     * @param stringRedisTemplate the auto-configured redis template by spring container
     * @return ServerRegistry<Supervisor, Worker>
     * @see org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
     */
    @Bean
    public ServerRegistry<Supervisor, Worker> supervisorRegistry(StringRedisTemplate stringRedisTemplate) {
        return new RedisSupervisorRegistry(stringRedisTemplate);
    }

    @Bean
    public TaskDispatcher taskDispatcher(Discovery<Worker> discoveryWorker,
                                         @Nullable TimingWheel<ExecuteParam> timingWheel) {
        DiscoveryRestTemplate<Worker> discoveryRestTemplate = DiscoveryRestTemplate.<Worker>builder()
            .connectTimeout(2000)
            .readTimeout(5000)
            .objectMapper(Jsons.createObjectMapper(JsonInclude.Include.NON_NULL))
            .discoveryServer(discoveryWorker)
            .maxRetryTimes(3)
            .build();
        return new HttpTaskDispatcher(discoveryRestTemplate, timingWheel);
    }

    @Bean
    public SupervisorStartup supervisorStartup(@Value("${server.port}") int port,
                                               @Value("${" + DISTRIBUTED_SCHEDULER_SUPERVISOR + ".job.heartbeat-interval-seconds:3}") int jobHeartbeatIntervalSeconds,
                                               @Value("${" + DISTRIBUTED_SCHEDULER_SUPERVISOR + ".track.heartbeat-interval-seconds:3}") int trackHeartbeatIntervalSeconds,
                                               JobManager jobManager,
                                               IdGenerator idGenerator,
                                               @Qualifier(SPRING_BEAN_NAME_SCAN_JOB_LOCKED) DoInLocked scanJobLocked,
                                               @Qualifier(SPRING_BEAN_NAME_SCAN_TRACK_LOCKED) DoInLocked scanTrackLocked,
                                               ServerRegistry<Supervisor, Worker> supervisorRegistry,
                                               TaskDispatcher taskDispatcher) {
        SupervisorStartup supervisorStartup = SupervisorStartup.builder()
            .port(port)
            .jobHeartbeatIntervalSeconds(jobHeartbeatIntervalSeconds)
            .trackHeartbeatIntervalSeconds(trackHeartbeatIntervalSeconds)
            .supervisorRegistry(supervisorRegistry)
            .jobManager(jobManager)
            .scanJobLocked(scanJobLocked)
            .scanTrackLocked(scanTrackLocked)
            .idGenerator(idGenerator)
            .taskDispatcher(taskDispatcher)
            .build();

        supervisorStartup.start();
        return supervisorStartup;
    }

    /**
     * Destroy job supervisor thread heartbeat.
     */
    @Configuration
    private static class SupervisorDisposableBean implements DisposableBean {
        private final SupervisorStartup supervisorStartup;

        public SupervisorDisposableBean(SupervisorStartup supervisorStartup) {
            this.supervisorStartup = supervisorStartup;
        }

        @Override
        public void destroy() {
            supervisorStartup.close();
        }
    }

}

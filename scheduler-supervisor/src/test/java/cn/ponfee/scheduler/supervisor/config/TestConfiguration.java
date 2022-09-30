package cn.ponfee.scheduler.supervisor.config;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.dispatch.TaskDispatcher;
import cn.ponfee.scheduler.dispatch.redis.RedisTaskDispatcher;
import cn.ponfee.scheduler.registry.ServerRegistry;
import cn.ponfee.scheduler.registry.redis.RedisSupervisorRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;

/**
 * Job supervisor configuration.
 *
 * @author Ponfee
 */
@Configuration
public class TestConfiguration {

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
    public TaskDispatcher taskDispatcher(RedisTemplate<String, String> redisTemplate,
                                         ServerRegistry<Supervisor, Worker> supervisorRegistry,
                                         @Nullable TimingWheel<ExecuteParam> timingWheel) {
        return new RedisTaskDispatcher(redisTemplate, supervisorRegistry, timingWheel);
    }

}

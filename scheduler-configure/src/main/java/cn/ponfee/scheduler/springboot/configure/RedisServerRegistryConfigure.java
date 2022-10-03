package cn.ponfee.scheduler.springboot.configure;

import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import cn.ponfee.scheduler.registry.WorkerRegistry;
import cn.ponfee.scheduler.registry.redis.RedisServerRegistry;
import cn.ponfee.scheduler.registry.redis.RedisSupervisorRegistry;
import cn.ponfee.scheduler.registry.redis.RedisWorkerRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;

import static cn.ponfee.scheduler.core.base.JobConstants.SPRING_BEAN_NAME_CURRENT_SUPERVISOR;
import static cn.ponfee.scheduler.core.base.JobConstants.SPRING_BEAN_NAME_CURRENT_WORKER;

/**
 * Redis server register & discovery configuration.
 *
 * @author Ponfee
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({StringRedisTemplate.class, RedisServerRegistry.class})
public class RedisServerRegistryConfigure {

    /**
     * Configuration redis supervisor registry.
     */
    @Configuration(proxyBeanMethods = false)
    @AutoConfigureAfter({EnableSupervisor.SupervisorConfiguration.class})
    @AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
    @DependsOn(SPRING_BEAN_NAME_CURRENT_SUPERVISOR)
    @ConditionalOnBean({Supervisor.class})
    public static class RedisSupervisorRegistryConfiguration {

        /**
         * RedisAutoConfiguration has auto-configured two redis template objects.
         * <p>RedisTemplate<Object, Object> redisTemplate
         * <p>StringRedisTemplate           stringRedisTemplate
         *
         * @param stringRedisTemplate the auto-configured redis template by spring container
         * @return SupervisorRegistry
         * @see org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
         */
        @Bean
        @ConditionalOnMissingBean
        public SupervisorRegistry supervisorRegistry(StringRedisTemplate stringRedisTemplate) {
            return new RedisSupervisorRegistry(stringRedisTemplate);
        }
    }

    /**
     * Configuration redis worker registry.
     */
    @Configuration(proxyBeanMethods = false)
    @AutoConfigureAfter({EnableWorker.WorkerConfiguration.class})
    @AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
    @DependsOn(SPRING_BEAN_NAME_CURRENT_WORKER)
    @ConditionalOnBean({Worker.class})
    public static class RedisWorkerRegistryConfiguration {
        @Bean
        @ConditionalOnMissingBean
        public WorkerRegistry workerRegistry(StringRedisTemplate stringRedisTemplate) {
            return new RedisWorkerRegistry(stringRedisTemplate);
        }
    }

}

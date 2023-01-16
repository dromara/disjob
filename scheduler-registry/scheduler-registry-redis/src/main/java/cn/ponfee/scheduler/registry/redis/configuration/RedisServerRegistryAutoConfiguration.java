package cn.ponfee.scheduler.registry.redis.configuration;

import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import cn.ponfee.scheduler.registry.WorkerRegistry;
import cn.ponfee.scheduler.registry.configuration.MarkServerRegistryAutoConfiguration;
import cn.ponfee.scheduler.registry.redis.RedisSupervisorRegistry;
import cn.ponfee.scheduler.registry.redis.RedisWorkerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Spring autoconfiguration for redis server registry
 *
 * @author Ponfee
 */
@EnableConfigurationProperties(RedisRegistryProperties.class)
public class RedisServerRegistryAutoConfiguration extends MarkServerRegistryAutoConfiguration {

    /**
     * Configuration redis supervisor registry.
     * <p>RedisAutoConfiguration has auto-configured two redis template objects.
     * <p>RedisTemplate<Object, Object> redisTemplate
     * <p>StringRedisTemplate           stringRedisTemplate
     *
     * @param stringRedisTemplate the auto-configured redis template by spring container
     * @return SupervisorRegistry
     * @see org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
     */
    @ConditionalOnBean(Supervisor.class) // 如果注解没有入参，则默认以方法的返回类型判断，即容器中已存在类型为SupervisorRegistry的实例才创建
    @ConditionalOnMissingBean            // 如果注解没有入参，则默认以方法的返回类型判断，即容器中不存在类型为SupervisorRegistry的实例才创建
    @Bean
    public SupervisorRegistry supervisorRegistry(@Value("${" + JobConstants.SCHEDULER_NAMESPACE + ":}") String namespace,
                                                 StringRedisTemplate stringRedisTemplate,
                                                 RedisRegistryProperties config) {
        return new RedisSupervisorRegistry(namespace, stringRedisTemplate, config);
    }

    /**
     * Configuration redis worker registry.
     */
    @ConditionalOnBean(Worker.class)
    @ConditionalOnMissingBean
    @Bean
    public WorkerRegistry workerRegistry(@Value("${" + JobConstants.SCHEDULER_NAMESPACE + ":}") String namespace,
                                         StringRedisTemplate stringRedisTemplate,
                                         RedisRegistryProperties config) {
        return new RedisWorkerRegistry(namespace, stringRedisTemplate, config);
    }

}

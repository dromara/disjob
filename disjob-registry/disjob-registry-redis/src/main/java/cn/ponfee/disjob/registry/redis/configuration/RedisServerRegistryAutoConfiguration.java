/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.registry.redis.configuration;

import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.registry.configuration.BaseServerRegistryAutoConfiguration;
import cn.ponfee.disjob.registry.redis.RedisSupervisorRegistry;
import cn.ponfee.disjob.registry.redis.RedisWorkerRegistry;
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
public class RedisServerRegistryAutoConfiguration extends BaseServerRegistryAutoConfiguration {

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
    public SupervisorRegistry supervisorRegistry(StringRedisTemplate stringRedisTemplate,
                                                 RedisRegistryProperties config) {
        return new RedisSupervisorRegistry(stringRedisTemplate, config);
    }

    /**
     * Configuration redis worker registry.
     */
    @ConditionalOnBean(Worker.class)
    @ConditionalOnMissingBean
    @Bean
    public WorkerRegistry workerRegistry(StringRedisTemplate stringRedisTemplate,
                                         RedisRegistryProperties config) {
        return new RedisWorkerRegistry(stringRedisTemplate, config);
    }

}

/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.registry.redis.configuration;

import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import cn.ponfee.scheduler.registry.WorkerRegistry;
import cn.ponfee.scheduler.registry.redis.RedisSupervisorRegistry;
import cn.ponfee.scheduler.registry.redis.RedisWorkerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.lang.annotation.*;

/**
 * Enable redis server registry
 *
 * @author Ponfee
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@EnableConfigurationProperties(RedisRegistryProperties.class)
@Import(EnableRedisServerRegistry.RedisServerRegistryConfigure.class)
public @interface EnableRedisServerRegistry {

    class RedisServerRegistryConfigure {
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
        @DependsOn(JobConstants.SPRING_BEAN_NAME_CURRENT_SUPERVISOR)
        @ConditionalOnBean(Supervisor.class)
        @ConditionalOnMissingBean
        @Bean
        public SupervisorRegistry supervisorRegistry(@Value("${" + JobConstants.SCHEDULER_NAMESPACE + ":}") String namespace,
                                                     StringRedisTemplate stringRedisTemplate,
                                                     RedisRegistryProperties config) {
            return new RedisSupervisorRegistry(namespace, stringRedisTemplate, config);
        }

        /**
         * Configuration redis worker registry.
         */
        @DependsOn(JobConstants.SPRING_BEAN_NAME_CURRENT_WORKER)
        @ConditionalOnBean(Worker.class)
        @ConditionalOnMissingBean
        @Bean
        public WorkerRegistry workerRegistry(@Value("${" + JobConstants.SCHEDULER_NAMESPACE + ":}") String namespace,
                                             StringRedisTemplate stringRedisTemplate,
                                             RedisRegistryProperties config) {
            return new RedisWorkerRegistry(namespace, stringRedisTemplate, config);
        }
    }

}

/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.config;

import cn.ponfee.disjob.common.base.IdGenerator;
import cn.ponfee.disjob.common.base.Snowflake;
import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.core.param.ExecuteTaskParam;
import cn.ponfee.disjob.dispatch.TaskDispatcher;
import cn.ponfee.disjob.dispatch.redis.RedisTaskDispatcher;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.registry.redis.RedisSupervisorRegistry;
import cn.ponfee.disjob.registry.redis.configuration.RedisRegistryProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@EnableConfigurationProperties(RedisRegistryProperties.class)
@Configuration
public class TestConfiguration {

    /**
     * RedisAutoConfiguration has auto-configured two redis template objects.
     * <p>RedisTemplate<Object, Object> redisTemplate
     * <p>StringRedisTemplate stringRedisTemplate
     *
     * @param stringRedisTemplate the auto-configured redis template by spring container
     * @param config              redis registry configuration
     * @return SupervisorRegistry
     * @see org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
     */
    @Bean
    public SupervisorRegistry supervisorRegistry(StringRedisTemplate stringRedisTemplate,
                                                 RedisRegistryProperties config) {
        return new RedisSupervisorRegistry(stringRedisTemplate, config);
    }

    @Bean
    public TaskDispatcher taskDispatcher(SupervisorRegistry supervisorRegistry,
                                         @Nullable TimingWheel<ExecuteTaskParam> timingWheel,
                                         RedisTemplate<String, String> redisTemplate) {
        return new RedisTaskDispatcher(supervisorRegistry, timingWheel, redisTemplate);
    }

    @Bean
    public IdGenerator idGenerator() {
        return new Snowflake(1);
    }

}

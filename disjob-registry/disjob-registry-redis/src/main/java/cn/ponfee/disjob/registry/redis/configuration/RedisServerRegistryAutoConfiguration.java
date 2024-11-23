/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.registry.redis.configuration;

import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.registry.configuration.BaseServerRegistryAutoConfiguration;
import cn.ponfee.disjob.registry.redis.RedisSupervisorRegistry;
import cn.ponfee.disjob.registry.redis.RedisWorkerRegistry;
import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.client.RestTemplate;

import static cn.ponfee.disjob.core.base.JobConstants.SPRING_BEAN_NAME_PREFIX;

/**
 * Spring autoconfiguration for redis server registry
 *
 * @author Ponfee
 */
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@EnableConfigurationProperties(RedisRegistryProperties.class)
public class RedisServerRegistryAutoConfiguration extends BaseServerRegistryAutoConfiguration {

    /**
     * Redis registry StringRedisTemplate spring bean name
     */
    public static final String SPRING_BEAN_NAME_STRING_REDIS_TEMPLATE_WRAPPER = SPRING_BEAN_NAME_PREFIX + ".registry.redis.string-redis-template-wrapper";

    /**
     * <pre>
     * RedisAutoConfiguration has auto-configured two redis template objects.
     *   1) RedisTemplate<Object, Object> redisTemplate
     *   2) StringRedisTemplate           stringRedisTemplate
     * </pre>
     *
     * @param stringRedisTemplate the auto-configured redis template by spring container
     * @return MutableObject
     */
    @ConditionalOnMissingBean(name = SPRING_BEAN_NAME_STRING_REDIS_TEMPLATE_WRAPPER)
    @Bean(SPRING_BEAN_NAME_STRING_REDIS_TEMPLATE_WRAPPER)
    public MutableObject<StringRedisTemplate> stringRedisTemplateWrapper(StringRedisTemplate stringRedisTemplate) {
        return new MutableObject<>(stringRedisTemplate);
    }

    /**
     * Configuration redis supervisor registry.
     * <p>@ConditionalOnBean：如果注解没有入参，则默认以方法的返回类型判断，即容器中已存在类型为SupervisorRegistry的实例才创建
     *
     * @param config                     redis registry configuration
     * @param restTemplate               the rest template
     * @param stringRedisTemplateWrapper the string redis template wrapper
     * @return SupervisorRegistry
     * @see org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
     */
    @ConditionalOnBean(Supervisor.Local.class)
    @Bean
    public SupervisorRegistry supervisorRegistry(RedisRegistryProperties config,
                                                 @Qualifier(JobConstants.SPRING_BEAN_NAME_REST_TEMPLATE) RestTemplate restTemplate,
                                                 @Qualifier(SPRING_BEAN_NAME_STRING_REDIS_TEMPLATE_WRAPPER) MutableObject<StringRedisTemplate> stringRedisTemplateWrapper) {
        return new RedisSupervisorRegistry(config, restTemplate, stringRedisTemplateWrapper.getValue());
    }

    /**
     * Configuration redis worker registry.
     */
    @ConditionalOnBean(Worker.Local.class)
    @Bean
    public WorkerRegistry workerRegistry(RedisRegistryProperties config,
                                         @Qualifier(JobConstants.SPRING_BEAN_NAME_REST_TEMPLATE) RestTemplate restTemplate,
                                         @Qualifier(SPRING_BEAN_NAME_STRING_REDIS_TEMPLATE_WRAPPER) MutableObject<StringRedisTemplate> stringRedisTemplateWrapper) {
        return new RedisWorkerRegistry(config, restTemplate, stringRedisTemplateWrapper.getValue());
    }

}

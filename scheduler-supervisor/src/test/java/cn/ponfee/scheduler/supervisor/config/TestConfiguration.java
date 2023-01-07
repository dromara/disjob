/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.config;

import cn.ponfee.scheduler.common.base.IdGenerator;
import cn.ponfee.scheduler.common.base.Snowflake;
import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.WorkerLocal;
import cn.ponfee.scheduler.core.base.WorkerService;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.dispatch.TaskDispatcher;
import cn.ponfee.scheduler.dispatch.redis.RedisTaskDispatcher;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
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
     * @return SupervisorRegistry
     * @see org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
     */
    @Bean
    public SupervisorRegistry supervisorRegistry(StringRedisTemplate stringRedisTemplate) {
        return new RedisSupervisorRegistry("", stringRedisTemplate);
    }

    @Bean
    public TaskDispatcher taskDispatcher(SupervisorRegistry supervisorRegistry,
                                         @Nullable TimingWheel<ExecuteParam> timingWheel,
                                         RedisTemplate<String, String> redisTemplate) {
        return new RedisTaskDispatcher(supervisorRegistry, timingWheel, redisTemplate);
    }

    @Bean
    public IdGenerator idGenerator() {
        return new Snowflake(1);
    }

    @Bean(JobConstants.SPRING_BEAN_NAME_WORKER_CLIENT)
    public WorkerService workerClient() {
        return new WorkerLocal();
    }
}

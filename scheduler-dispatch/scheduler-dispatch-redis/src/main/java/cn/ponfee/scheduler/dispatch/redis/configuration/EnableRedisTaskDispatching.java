/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.dispatch.redis.configuration;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.dispatch.TaskDispatcher;
import cn.ponfee.scheduler.dispatch.TaskReceiver;
import cn.ponfee.scheduler.dispatch.redis.RedisTaskDispatcher;
import cn.ponfee.scheduler.dispatch.redis.RedisTaskReceiver;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;

import java.lang.annotation.*;

/**
 * Enable redis task dispatch
 *
 * @author Ponfee
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(EnableRedisTaskDispatching.RedisTaskDispatchingConfiguration.class)
public @interface EnableRedisTaskDispatching {

    class RedisTaskDispatchingConfiguration {
        /**
         * Configuration redis task dispatcher.
         */
        @DependsOn(JobConstants.SPRING_BEAN_NAME_CURRENT_SUPERVISOR)
        @ConditionalOnBean(Supervisor.class)
        @ConditionalOnMissingBean
        @Bean
        public TaskDispatcher taskDispatcher(SupervisorRegistry discoveryWorker,
                                             @Nullable TimingWheel<ExecuteParam> timingWheel,
                                             StringRedisTemplate stringRedisTemplate) {
            return new RedisTaskDispatcher(discoveryWorker, timingWheel, stringRedisTemplate);
        }

        /**
         * Configuration redis task receiver.
         */
        @DependsOn({JobConstants.SPRING_BEAN_NAME_CURRENT_WORKER, JobConstants.SPRING_BEAN_NAME_TIMING_WHEEL})
        @ConditionalOnBean({Worker.class, TimingWheel.class})
        @ConditionalOnMissingBean
        @Bean
        public TaskReceiver taskReceiver(Worker worker,
                                         TimingWheel<ExecuteParam> timingWheel,
                                         StringRedisTemplate stringRedisTemplate) {
            return new RedisTaskReceiver(worker, timingWheel, stringRedisTemplate);
        }
    }

}

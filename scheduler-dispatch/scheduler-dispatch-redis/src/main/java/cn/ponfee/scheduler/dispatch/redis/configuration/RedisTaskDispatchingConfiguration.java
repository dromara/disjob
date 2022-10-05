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
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;

/**
 * Redis task dispatcher & receiver configuration.
 *
 * @author Ponfee
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({StringRedisTemplate.class})
public class RedisTaskDispatchingConfiguration {

    /**
     * Configuration redis task dispatcher.
     */
    @Configuration(proxyBeanMethods = false)
    @AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
    @DependsOn(JobConstants.SPRING_BEAN_NAME_CURRENT_SUPERVISOR)
    @ConditionalOnBean({Supervisor.class})
    public static class RedisTaskDispatcherConfiguration {
        @Bean
        @ConditionalOnMissingBean
        public TaskDispatcher taskDispatcher(SupervisorRegistry discoveryWorker,
                                             @Nullable TimingWheel<ExecuteParam> timingWheel,
                                             StringRedisTemplate stringRedisTemplate) {
            return new RedisTaskDispatcher(discoveryWorker, timingWheel, stringRedisTemplate);
        }
    }

    /**
     * Configuration redis task receiver.
     */
    @Configuration(proxyBeanMethods = false)
    @AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
    @DependsOn(JobConstants.SPRING_BEAN_NAME_CURRENT_WORKER)
    @ConditionalOnBean({Worker.class})
    @ConditionalOnSingleCandidate(TimingWheel.class)
    public static class RedisTaskReceiverConfiguration {
        @Bean
        @ConditionalOnMissingBean
        public TaskReceiver taskReceiver(Worker worker,
                                         TimingWheel<ExecuteParam> timingWheel,
                                         StringRedisTemplate stringRedisTemplate) {
            return new RedisTaskReceiver(worker, timingWheel, stringRedisTemplate);
        }
    }

}

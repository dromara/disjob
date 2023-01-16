package cn.ponfee.scheduler.dispatch.redis.configuration;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.dispatch.TaskDispatcher;
import cn.ponfee.scheduler.dispatch.TaskReceiver;
import cn.ponfee.scheduler.dispatch.configuration.MarkTaskDispatchingAutoConfiguration;
import cn.ponfee.scheduler.dispatch.redis.RedisTaskDispatcher;
import cn.ponfee.scheduler.dispatch.redis.RedisTaskReceiver;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;

/**
 * Spring autoconfiguration for redis task dispatching.
 *
 * @author Ponfee
 */
public class RedisTaskDispatchingAutoConfiguration extends MarkTaskDispatchingAutoConfiguration {

    /**
     * Configuration redis task dispatcher.
     */
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
    @ConditionalOnBean(Worker.class)
    @ConditionalOnMissingBean
    @Bean
    public TaskReceiver taskReceiver(Worker currentWorker,
                                     TimingWheel<ExecuteParam> timingWheel,
                                     StringRedisTemplate stringRedisTemplate) {
        return new RedisTaskReceiver(currentWorker, timingWheel, stringRedisTemplate);
    }

}

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

package cn.ponfee.disjob.dispatch.redis.configuration;

import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import cn.ponfee.disjob.dispatch.TaskDispatcher;
import cn.ponfee.disjob.dispatch.TaskReceiver;
import cn.ponfee.disjob.dispatch.configuration.BaseTaskDispatchingAutoConfiguration;
import cn.ponfee.disjob.dispatch.redis.RedisTaskDispatcher;
import cn.ponfee.disjob.dispatch.redis.RedisTaskReceiver;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;

/**
 * Spring autoconfiguration for redis task dispatching.
 *
 * @author Ponfee
 */
public class RedisTaskDispatchingAutoConfiguration extends BaseTaskDispatchingAutoConfiguration {

    /**
     * Configuration redis task receiver.
     */
    @ConditionalOnBean(Worker.Current.class)
    @Bean
    public TaskReceiver taskReceiver(Worker.Current currentWorker,
                                     TimingWheel<ExecuteTaskParam> timingWheel,
                                     StringRedisTemplate stringRedisTemplate) {
        return new RedisTaskReceiver(currentWorker, timingWheel, stringRedisTemplate);
    }

    /**
     * Configuration redis task dispatcher.
     */
    @ConditionalOnBean(Supervisor.Current.class)
    @Bean
    public TaskDispatcher taskDispatcher(ApplicationEventPublisher eventPublisher,
                                         SupervisorRegistry discoveryWorker,
                                         RetryProperties retryProperties,
                                         StringRedisTemplate stringRedisTemplate,
                                         @Nullable RedisTaskReceiver taskReceiver) {
        return new RedisTaskDispatcher(eventPublisher, discoveryWorker, retryProperties, stringRedisTemplate, taskReceiver);
    }

}

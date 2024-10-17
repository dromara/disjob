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

package cn.ponfee.disjob.dispatch.http.configuration;

import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import cn.ponfee.disjob.dispatch.TaskDispatcher;
import cn.ponfee.disjob.dispatch.TaskReceiver;
import cn.ponfee.disjob.dispatch.configuration.BaseTaskDispatchingAutoConfiguration;
import cn.ponfee.disjob.dispatch.http.HttpTaskDispatcher;
import cn.ponfee.disjob.dispatch.http.HttpTaskReceiver;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RestTemplate;

/**
 * Spring autoconfiguration for http task dispatching.
 *
 * @author Ponfee
 */
public class HttpTaskDispatchingAutoConfiguration extends BaseTaskDispatchingAutoConfiguration {

    /**
     * Configuration http task receiver.
     */
    @ConditionalOnBean(Worker.Local.class)
    @Bean
    public TaskReceiver taskReceiver(Worker.Local localWorker,
                                     @Qualifier(JobConstants.SPRING_BEAN_NAME_TIMING_WHEEL) TimingWheel<ExecuteTaskParam> timingWheel) {
        return new HttpTaskReceiver(localWorker, timingWheel);
    }

    /**
     * Configuration http task dispatcher.
     */
    @ConditionalOnBean(Supervisor.Local.class)
    @Bean
    public TaskDispatcher taskDispatcher(ApplicationEventPublisher eventPublisher,
                                         SupervisorRegistry discoveryWorker,
                                         RetryProperties retry,
                                         Supervisor.Local localSupervisor,
                                         @Qualifier(JobConstants.SPRING_BEAN_NAME_REST_TEMPLATE) RestTemplate restTemplate,
                                         @Nullable TaskReceiver taskReceiver) {
        return new HttpTaskDispatcher(
            eventPublisher, discoveryWorker, retry, localSupervisor, restTemplate, (HttpTaskReceiver) taskReceiver);
    }

}

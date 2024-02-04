/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.dispatch.http.configuration;

import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.common.spring.RestTemplateUtils;
import cn.ponfee.disjob.core.base.HttpProperties;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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
    @ConditionalOnBean(Worker.Current.class)
    @Bean
    public TaskReceiver taskReceiver(Worker.Current currentWorker, TimingWheel<ExecuteTaskParam> timingWheel) {
        return new HttpTaskReceiver(currentWorker, timingWheel);
    }

    /**
     * Configuration http task dispatcher.
     */
    @ConditionalOnClass(RestTemplate.class)
    @ConditionalOnBean(Supervisor.Current.class)
    @Bean
    public TaskDispatcher taskDispatcher(HttpProperties http,
                                         RetryProperties retry,
                                         SupervisorRegistry discoveryWorker,
                                         @Nullable ObjectMapper objectMapper,
                                         @Nullable TaskReceiver taskReceiver) {
        http.check();
        RestTemplate restTemplate = RestTemplateUtils.create(http.getConnectTimeout(), http.getReadTimeout(), objectMapper);
        return new HttpTaskDispatcher(discoveryWorker, retry, restTemplate, taskReceiver);
    }

}

/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.dispatch.http.configuration;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.base.HttpProperties;
import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.dispatch.TaskDispatcher;
import cn.ponfee.scheduler.dispatch.TaskReceiver;
import cn.ponfee.scheduler.dispatch.configuration.MarkTaskDispatchingAutoConfiguration;
import cn.ponfee.scheduler.dispatch.http.HttpTaskDispatcher;
import cn.ponfee.scheduler.dispatch.http.HttpTaskReceiver;
import cn.ponfee.scheduler.registry.DiscoveryRestTemplate;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RestTemplate;

/**
 * Spring autoconfiguration for http task dispatching.
 *
 * @author Ponfee
 */
public class HttpTaskDispatchingAutoConfiguration extends MarkTaskDispatchingAutoConfiguration {

    /**
     * Configuration http task dispatcher.
     */
    @ConditionalOnClass(RestTemplate.class)
    @ConditionalOnBean(Supervisor.class)
    @ConditionalOnMissingBean
    @Bean
    public TaskDispatcher taskDispatcher(HttpProperties properties,
                                         SupervisorRegistry discoveryWorker,
                                         @Nullable TimingWheel<ExecuteParam> timingWheel,
                                         @Nullable @Qualifier(JobConstants.SPRING_BEAN_NAME_OBJECT_MAPPER) ObjectMapper objectMapper) {
        DiscoveryRestTemplate<Worker> discoveryRestTemplate = DiscoveryRestTemplate.<Worker>builder()
            .connectTimeout(properties.getConnectTimeout())
            .readTimeout(properties.getReadTimeout())
            .maxRetryTimes(properties.getMaxRetryTimes())
            .objectMapper(objectMapper != null ? objectMapper : Jsons.createObjectMapper(JsonInclude.Include.NON_NULL))
            .discoveryServer(discoveryWorker)
            .build();

        return new HttpTaskDispatcher(discoveryRestTemplate, timingWheel);
    }

    /**
     * Configuration http task receiver.
     */
    @ConditionalOnBean(Worker.class)
    @ConditionalOnMissingBean
    @Bean
    public TaskReceiver taskReceiver(TimingWheel<ExecuteParam> timingWheel) {
        return new HttpTaskReceiver(timingWheel);
    }

}

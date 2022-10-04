package cn.ponfee.scheduler.springboot.configure;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.base.HttpProperties;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.dispatch.TaskDispatcher;
import cn.ponfee.scheduler.dispatch.TaskReceiver;
import cn.ponfee.scheduler.dispatch.http.HttpTaskDispatcher;
import cn.ponfee.scheduler.dispatch.http.HttpTaskReceiver;
import cn.ponfee.scheduler.registry.DiscoveryRestTemplate;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RestTemplate;

import static cn.ponfee.scheduler.core.base.JobConstants.*;

/**
 * Http task dispatcher and receiver configuration.
 * <p>Note: AutoConfigureAfter需要配置为自动装配时才生效(META-INF/spring.factories)
 *
 * @author Ponfee
 */
@Configuration(proxyBeanMethods = false)
public class HttpTaskDispatchingConfiguration {

    /**
     * Configuration http task dispatcher.
     */
    @Configuration(proxyBeanMethods = false)
    @AutoConfigureAfter({EnableSupervisor.SupervisorConfiguration.class})
    @AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
    @ConditionalOnClass({RestTemplate.class})
    @DependsOn(SPRING_BEAN_NAME_CURRENT_SUPERVISOR)
    @ConditionalOnBean({Supervisor.class})
    public static class HttpTaskDispatcherConfiguration {
        @Bean
        @ConditionalOnMissingBean
        public TaskDispatcher taskDispatcher(HttpProperties properties,
                                             SupervisorRegistry supervisorRegistry,
                                             @Nullable TimingWheel<ExecuteParam> timingWheel,
                                             @Nullable @Qualifier(SPRING_BEAN_NAME_OBJECT_MAPPER) ObjectMapper objectMapper) {
            DiscoveryRestTemplate<Worker> discoveryRestTemplate = DiscoveryRestTemplate.<Worker>builder()
                .connectTimeout(properties.getConnectTimeout())
                .readTimeout(properties.getReadTimeout())
                .maxRetryTimes(properties.getMaxRetryTimes())
                .objectMapper(objectMapper != null ? objectMapper : Jsons.createObjectMapper(JsonInclude.Include.NON_NULL))
                .discoveryServer(supervisorRegistry)
                .build();

            return new HttpTaskDispatcher(discoveryRestTemplate, timingWheel);
        }
    }

    /**
     * Configuration http task receiver.
     */
    @Configuration(proxyBeanMethods = false)
    @AutoConfigureAfter({EnableWorker.WorkerConfiguration.class})
    @AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
    @DependsOn(SPRING_BEAN_NAME_CURRENT_WORKER)
    @ConditionalOnBean(Worker.class)
    @ConditionalOnSingleCandidate(TimingWheel.class)
    public static class HttpTaskReceiverConfiguration {
        @Bean
        @ConditionalOnMissingBean
        public TaskReceiver taskReceiver(TimingWheel<ExecuteParam> timingWheel) {
            return new HttpTaskReceiver(timingWheel);
        }
    }

}

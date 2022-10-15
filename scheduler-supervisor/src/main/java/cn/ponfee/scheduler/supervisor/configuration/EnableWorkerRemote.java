package cn.ponfee.scheduler.supervisor.configuration;

import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.base.HttpProperties;
import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.base.WorkerService;
import cn.ponfee.scheduler.registry.DiscoveryRestProxy;
import cn.ponfee.scheduler.registry.DiscoveryRestTemplate;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.lang.Nullable;

import java.lang.annotation.*;

import static cn.ponfee.scheduler.core.base.JobConstants.SPRING_BEAN_NAME_OBJECT_MAPPER;

/**
 * Enable worker service remote implementation.
 *
 * @author Ponfee
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(EnableWorkerRemote.WorkerRemoteConfiguration.class)
public @interface EnableWorkerRemote {

    @ConditionalOnClass({WorkerService.class})
    class WorkerRemoteConfiguration {
        @Bean(JobConstants.SPRING_BEAN_NAME_WORKER_CLIENT)
        @ConditionalOnMissingBean
        public WorkerService workerClient(HttpProperties properties,
                                          SupervisorRegistry supervisorRegistry,
                                          @Nullable @Qualifier(SPRING_BEAN_NAME_OBJECT_MAPPER) ObjectMapper objectMapper) {
            DiscoveryRestTemplate<Worker> discoveryRestTemplate = DiscoveryRestTemplate.<Worker>builder()
                .connectTimeout(properties.getConnectTimeout())
                .readTimeout(properties.getReadTimeout())
                .maxRetryTimes(properties.getMaxRetryTimes())
                .objectMapper(objectMapper != null ? objectMapper : Jsons.createObjectMapper(JsonInclude.Include.NON_NULL))
                .discoveryServer(supervisorRegistry)
                .build();
            return DiscoveryRestProxy.create(WorkerService.class, discoveryRestTemplate);
        }
    }

}

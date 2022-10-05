package cn.ponfee.scheduler.worker.samples.config;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.base.HttpProperties;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.SupervisorService;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.registry.DiscoveryRestProxy;
import cn.ponfee.scheduler.registry.DiscoveryRestTemplate;
import cn.ponfee.scheduler.registry.WorkerRegistry;
import cn.ponfee.scheduler.worker.base.TaskTimingWheel;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import static cn.ponfee.scheduler.core.base.JobConstants.SPRING_BEAN_NAME_OBJECT_MAPPER;

/**
 * Worker configuration
 *
 * @author Ponfee
 */
@Configuration
public class WorkerConfiguration {

    @Bean
    public TimingWheel<ExecuteParam> timingWheel() {
        return new TaskTimingWheel();
    }

    @Bean
    public SupervisorService supervisorServiceClient(HttpProperties properties,
                                                     WorkerRegistry workerRegistry,
                                                     @Nullable @Qualifier(SPRING_BEAN_NAME_OBJECT_MAPPER) ObjectMapper objectMapper) {
        DiscoveryRestTemplate<Supervisor> discoveryRestTemplate = DiscoveryRestTemplate.<Supervisor>builder()
            .connectTimeout(properties.getConnectTimeout())
            .readTimeout(properties.getReadTimeout())
            .maxRetryTimes(properties.getMaxRetryTimes())
            .objectMapper(objectMapper != null ? objectMapper : Jsons.createObjectMapper(JsonInclude.Include.NON_NULL))
            .discoveryServer(workerRegistry)
            .build();
        return DiscoveryRestProxy.create(SupervisorService.class, discoveryRestTemplate);
    }

}

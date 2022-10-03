package cn.ponfee.scheduler.worker.samples.config;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.common.util.Jsons;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import static cn.ponfee.scheduler.core.base.JobConstants.*;

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
    public SupervisorService supervisorServiceClient(@Value("${" + HTTP_CONNECT_TIMEOUT_KEY + ":2000}") int connectTimeout,
                                                     @Value("${" + HTTP_READ_TIMEOUT_KEY + ":5000}") int readTimeout,
                                                     @Value("${" + HTTP_MAX_RETRY_TIMES_KEY + ":3}") int maxRetryTimes,
                                                     WorkerRegistry workerRegistry,
                                                     @Nullable @Qualifier(HTTP_OBJECT_MAPPER_SPRING_BEAN_NAME) ObjectMapper objectMapper) {
        DiscoveryRestTemplate<Supervisor> discoveryRestTemplate = DiscoveryRestTemplate.<Supervisor>builder()
            .connectTimeout(connectTimeout)
            .readTimeout(readTimeout)
            .maxRetryTimes(maxRetryTimes)
            .objectMapper(objectMapper != null ? objectMapper : Jsons.createObjectMapper(JsonInclude.Include.NON_NULL))
            .discoveryServer(workerRegistry)
            .build();
        return DiscoveryRestProxy.create(SupervisorService.class, discoveryRestTemplate);
    }

}

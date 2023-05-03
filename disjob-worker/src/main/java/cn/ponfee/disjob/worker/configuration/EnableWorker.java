/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.worker.configuration;

import cn.ponfee.disjob.common.spring.SpringContextHolder;
import cn.ponfee.disjob.common.util.ClassUtils;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.common.util.ObjectUtils;
import cn.ponfee.disjob.core.base.*;
import cn.ponfee.disjob.core.util.JobUtils;
import cn.ponfee.disjob.registry.DiscoveryRestProxy;
import cn.ponfee.disjob.registry.DiscoveryRestTemplate;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.worker.base.TaskTimingWheel;
import cn.ponfee.disjob.worker.rpc.WorkerServiceProvider;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.Nullable;

import java.lang.annotation.*;

/**
 * Enable worker role
 * <p>必须注解到具有@Component的类上且该类能被spring扫描到
 *
 * @author Ponfee
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@EnableConfigurationProperties(WorkerProperties.class)
@Import({
    EnableWorker.EnableHttpProperties.class,
    EnableWorker.EnableWorkerConfiguration.class,
    WorkerStartupRunner.class,
})
public @interface EnableWorker {

    @ConditionalOnMissingBean(HttpProperties.class)
    @EnableConfigurationProperties(HttpProperties.class)
    class EnableHttpProperties {
    }

    @ConditionalOnProperty(JobConstants.SPRING_WEB_SERVER_PORT)
    class EnableWorkerConfiguration {
        @AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
        @Order(Ordered.HIGHEST_PRECEDENCE)
        @ConditionalOnMissingBean
        @Bean(JobConstants.SPRING_BEAN_NAME_TIMING_WHEEL)
        public TaskTimingWheel timingWheel(WorkerProperties config) {
            return new TaskTimingWheel(config.getTimingWheelTickMs(), config.getTimingWheelRingSize());
        }

        @AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
        @Order(Ordered.HIGHEST_PRECEDENCE)
        @DependsOn(JobConstants.SPRING_BEAN_NAME_TIMING_WHEEL)
        @ConditionalOnMissingBean
        @Bean(JobConstants.SPRING_BEAN_NAME_CURRENT_WORKER)
        public Worker currentWorker(@Value("${" + JobConstants.SPRING_WEB_SERVER_PORT + "}") int port,
                                    @Value("${" + JobConstants.DISJOB_BOUND_SERVER_HOST + ":}") String boundHost,
                                    WorkerProperties config) {
            String host = JobUtils.getLocalHost(boundHost);
            Worker currentWorker = new Worker(config.getGroup(), ObjectUtils.uuid32(), host, port);
            // inject current worker: Worker.class.getDeclaredClasses()[0]
            try {
                ClassUtils.invoke(Class.forName(Worker.class.getName() + "$Current"), "set", new Object[]{currentWorker});
            } catch (ClassNotFoundException e) {
                // cannot happen
                throw new AssertionError("Setting as current worker occur error.", e);
            }
            return currentWorker;
        }

        @DependsOn(JobConstants.SPRING_BEAN_NAME_CURRENT_WORKER)
        @ConditionalOnMissingClass("cn.ponfee.disjob.supervisor.manager.DistributedJobManager")
        @ConditionalOnMissingBean
        @Bean
        public SupervisorService supervisorServiceClient(HttpProperties httpConfig,
                                                         WorkerRegistry workerRegistry,
                                                         @Nullable @Qualifier(JobConstants.SPRING_BEAN_NAME_OBJECT_MAPPER) ObjectMapper objectMapper) {
            DiscoveryRestTemplate<Supervisor> discoveryRestTemplate = DiscoveryRestTemplate.<Supervisor>builder()
                .connectTimeout(httpConfig.getConnectTimeout())
                .readTimeout(httpConfig.getReadTimeout())
                .maxRetryTimes(httpConfig.getMaxRetryTimes())
                .objectMapper(objectMapper != null ? objectMapper : Jsons.createObjectMapper(JsonInclude.Include.NON_NULL))
                .discoveryServer(workerRegistry)
                .build();
            return DiscoveryRestProxy.create(false, SupervisorService.class, discoveryRestTemplate);
        }

        @DependsOn(JobConstants.SPRING_BEAN_NAME_CURRENT_WORKER)
        @ConditionalOnMissingBean
        @Bean
        public WorkerService workerService() {
            return new WorkerServiceProvider();
        }

        @ConditionalOnMissingBean
        @Bean
        public SpringContextHolder springContextHolder() {
            return new SpringContextHolder();
        }
    }

}

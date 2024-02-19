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

package cn.ponfee.disjob.worker.configuration;

import cn.ponfee.disjob.common.spring.LocalizedMethodArgumentConfigurer;
import cn.ponfee.disjob.common.spring.RestTemplateUtils;
import cn.ponfee.disjob.common.spring.SpringContextHolder;
import cn.ponfee.disjob.common.util.ClassUtils;
import cn.ponfee.disjob.common.util.UuidUtils;
import cn.ponfee.disjob.core.base.*;
import cn.ponfee.disjob.core.util.JobUtils;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.worker.base.TaskTimingWheel;
import cn.ponfee.disjob.worker.provider.WorkerRpcProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RestTemplate;

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
    EnableWorker.EnableRetryProperties.class,
    EnableWorker.EnableHttpProperties.class,
    EnableWorker.EnableWorkerConfiguration.class,
    WorkerLifecycle.class,
})
public @interface EnableWorker {

    @ConditionalOnMissingBean(RetryProperties.class)
    @EnableConfigurationProperties(RetryProperties.class)
    class EnableRetryProperties {
    }

    @ConditionalOnMissingBean(HttpProperties.class)
    @EnableConfigurationProperties(HttpProperties.class)
    class EnableHttpProperties {
    }

    @ConditionalOnProperty(JobConstants.SPRING_WEB_SERVER_PORT)
    class EnableWorkerConfiguration {

        @AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
        @Order(Ordered.HIGHEST_PRECEDENCE)
        @Bean(JobConstants.SPRING_BEAN_NAME_TIMING_WHEEL)
        public TaskTimingWheel timingWheel(WorkerProperties config) {
            return new TaskTimingWheel(config.getTimingWheelTickMs(), config.getTimingWheelRingSize());
        }

        @AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
        @Order(Ordered.HIGHEST_PRECEDENCE)
        @DependsOn(JobConstants.SPRING_BEAN_NAME_TIMING_WHEEL)
        @Bean(JobConstants.SPRING_BEAN_NAME_CURRENT_WORKER)
        public Worker.Current currentWorker(@Value("${" + JobConstants.SPRING_WEB_SERVER_PORT + "}") int port,
                                            @Value("${" + JobConstants.DISJOB_BOUND_SERVER_HOST + ":}") String boundHost,
                                            WorkerProperties config) {
            config.check();
            String host = JobUtils.getLocalHost(boundHost);
            String workerToken = config.getWorkerToken();
            String supervisorToken = config.getSupervisorToken();
            String supervisorContextPath = config.getSupervisorContextPath();
            Object[] args = {config.getGroup(), UuidUtils.uuid32(), host, port, workerToken, supervisorToken, supervisorContextPath};
            try {
                // inject current worker: Worker.class.getDeclaredClasses()[0]
                return ClassUtils.invoke(Class.forName(Worker.Current.class.getName()), "create", args);
            } catch (ClassNotFoundException e) {
                // cannot happen
                throw new Error("Setting as current worker occur error.", e);
            }
        }

        @ConditionalOnMissingBean
        @Bean(JobConstants.SPRING_BEAN_NAME_REST_TEMPLATE)
        public RestTemplate restTemplate(HttpProperties http, @Nullable ObjectMapper objectMapper) {
            http.check();
            return RestTemplateUtils.create(http.getConnectTimeout(), http.getReadTimeout(), objectMapper);
        }

        @DependsOn(JobConstants.SPRING_BEAN_NAME_CURRENT_WORKER)
        @Bean
        public WorkerRpcService workerRpcService(Worker.Current currentWork,
                                                 WorkerRegistry registry) {
            return new WorkerRpcProvider(currentWork, registry);
        }

        @ConditionalOnMissingBean
        @Bean
        public LocalizedMethodArgumentConfigurer localizedMethodArgumentConfigurer() {
            return new LocalizedMethodArgumentConfigurer();
        }

        @ConditionalOnMissingBean
        @Bean
        public SpringContextHolder springContextHolder() {
            return new SpringContextHolder();
        }
    }

}

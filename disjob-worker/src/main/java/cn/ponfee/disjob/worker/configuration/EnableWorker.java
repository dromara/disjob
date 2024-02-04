/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.worker.configuration;

import cn.ponfee.disjob.common.spring.LocalizedMethodArgumentConfigurer;
import cn.ponfee.disjob.common.spring.SpringContextHolder;
import cn.ponfee.disjob.common.util.ClassUtils;
import cn.ponfee.disjob.common.util.UuidUtils;
import cn.ponfee.disjob.core.base.*;
import cn.ponfee.disjob.core.util.JobUtils;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.worker.base.TaskTimingWheel;
import cn.ponfee.disjob.worker.provider.WorkerRpcProvider;
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

        @DependsOn(JobConstants.SPRING_BEAN_NAME_CURRENT_WORKER)
        @ConditionalOnMissingBean
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

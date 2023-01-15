/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.registry.consul.configuration;

import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import cn.ponfee.scheduler.registry.WorkerRegistry;
import cn.ponfee.scheduler.registry.consul.ConsulSupervisorRegistry;
import cn.ponfee.scheduler.registry.consul.ConsulWorkerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enable consul server registry
 *
 * @author Ponfee
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@EnableConfigurationProperties(ConsulRegistryProperties.class)
@Import(EnableConsulServerRegistry.ConsulServerRegistryConfigure.class)
public @interface EnableConsulServerRegistry {

    class ConsulServerRegistryConfigure {
        /**
         * Configuration consul supervisor registry.
         */
        @DependsOn(JobConstants.SPRING_BEAN_NAME_CURRENT_SUPERVISOR)
        @ConditionalOnBean(Supervisor.class)
        @ConditionalOnMissingBean
        @Bean
        public SupervisorRegistry supervisorRegistry(@Value("${" + JobConstants.SCHEDULER_NAMESPACE + ":}") String namespace,
                                                     ConsulRegistryProperties config) {
            return new ConsulSupervisorRegistry(namespace, config);
        }

        /**
         * Configuration consul worker registry.
         */
        @DependsOn(JobConstants.SPRING_BEAN_NAME_CURRENT_WORKER)
        @ConditionalOnBean(Worker.class)
        @ConditionalOnMissingBean
        @Bean
        public WorkerRegistry workerRegistry(@Value("${" + JobConstants.SCHEDULER_NAMESPACE + ":}") String namespace,
                                             ConsulRegistryProperties config) {
            return new ConsulWorkerRegistry(namespace, config);
        }
    }

}

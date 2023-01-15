/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.registry.nacos.configuration;

import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import cn.ponfee.scheduler.registry.WorkerRegistry;
import cn.ponfee.scheduler.registry.nacos.NacosSupervisorRegistry;
import cn.ponfee.scheduler.registry.nacos.NacosWorkerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enable nacos server registry
 *
 * @author Ponfee
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@EnableConfigurationProperties(NacosRegistryProperties.class)
@Import(EnableNacosServerRegistry.NacosServerRegistryConfigure.class)
public @interface EnableNacosServerRegistry {

    class NacosServerRegistryConfigure {
        /**
         * Configuration nacos supervisor registry.
         */
        @DependsOn(JobConstants.SPRING_BEAN_NAME_CURRENT_SUPERVISOR)
        @ConditionalOnBean(Supervisor.class)
        @ConditionalOnMissingBean
        @Bean
        public SupervisorRegistry supervisorRegistry(@Value("${" + JobConstants.SCHEDULER_NAMESPACE + ":}") String namespace,
                                                     NacosRegistryProperties config) {
            return new NacosSupervisorRegistry(namespace, config);
        }

        /**
         * Configuration nacos worker registry.
         */
        @DependsOn(JobConstants.SPRING_BEAN_NAME_CURRENT_WORKER)
        @ConditionalOnBean(Worker.class)
        @ConditionalOnMissingBean
        @Bean
        public WorkerRegistry workerRegistry(@Value("${" + JobConstants.SCHEDULER_NAMESPACE + ":}") String namespace,
                                             NacosRegistryProperties config) {
            return new NacosWorkerRegistry(namespace, config);
        }
    }

}

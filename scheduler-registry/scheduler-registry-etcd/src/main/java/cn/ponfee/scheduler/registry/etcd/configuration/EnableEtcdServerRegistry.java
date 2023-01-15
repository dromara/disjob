/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.registry.etcd.configuration;

import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import cn.ponfee.scheduler.registry.WorkerRegistry;
import cn.ponfee.scheduler.registry.etcd.EtcdSupervisorRegistry;
import cn.ponfee.scheduler.registry.etcd.EtcdWorkerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enable etcd server registry
 *
 * @author Ponfee
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@EnableConfigurationProperties(EtcdRegistryProperties.class)
@Import(EnableEtcdServerRegistry.EtcdServerRegistryConfigure.class)
public @interface EnableEtcdServerRegistry {

    class EtcdServerRegistryConfigure {
        /**
         * Configuration etcd supervisor registry.
         */
        @DependsOn(JobConstants.SPRING_BEAN_NAME_CURRENT_SUPERVISOR)
        @ConditionalOnBean(Supervisor.class)
        @ConditionalOnMissingBean
        @Bean
        public SupervisorRegistry supervisorRegistry(@Value("${" + JobConstants.SCHEDULER_NAMESPACE + ":}") String namespace,
                                                     EtcdRegistryProperties config) {
            return new EtcdSupervisorRegistry(namespace, config);
        }

        /**
         * Configuration etcd worker registry.
         */
        @DependsOn(JobConstants.SPRING_BEAN_NAME_CURRENT_WORKER)
        @ConditionalOnBean(Worker.class)
        @ConditionalOnMissingBean
        @Bean
        public WorkerRegistry workerRegistry(@Value("${" + JobConstants.SCHEDULER_NAMESPACE + ":}") String namespace,
                                             EtcdRegistryProperties config) {
            return new EtcdWorkerRegistry(namespace, config);
        }
    }

}

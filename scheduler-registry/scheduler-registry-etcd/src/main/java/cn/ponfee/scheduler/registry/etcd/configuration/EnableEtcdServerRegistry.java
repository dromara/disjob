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
import cn.ponfee.scheduler.registry.etcd.EtcdServerRegistry;
import cn.ponfee.scheduler.registry.etcd.EtcdSupervisorRegistry;
import cn.ponfee.scheduler.registry.etcd.EtcdWorkerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import java.lang.annotation.*;

import static cn.ponfee.scheduler.core.base.JobConstants.SPRING_BEAN_NAME_CURRENT_SUPERVISOR;
import static cn.ponfee.scheduler.core.base.JobConstants.SPRING_BEAN_NAME_CURRENT_WORKER;

/**
 * Enable etcd server registry
 *
 * @author Ponfee
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(EnableEtcdServerRegistry.EtcdServerRegistryConfigure.class)
public @interface EnableEtcdServerRegistry {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({EtcdServerRegistry.class})
    class EtcdServerRegistryConfigure {

        /**
         * Configuration etcd supervisor registry.
         */
        @Configuration(proxyBeanMethods = false)
        @AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
        @DependsOn(SPRING_BEAN_NAME_CURRENT_SUPERVISOR)
        @ConditionalOnBean({Supervisor.class})
        public static class EtcdSupervisorRegistryConfiguration {

            @Bean
            @ConditionalOnMissingBean
            public SupervisorRegistry supervisorRegistry(@Value("${" + JobConstants.SCHEDULER_NAMESPACE + ":}") String namespace,
                                                         EtcdRegistryProperties config) {
                return new EtcdSupervisorRegistry(namespace, config);
            }
        }

        /**
         * Configuration etcd worker registry.
         */
        @Configuration(proxyBeanMethods = false)
        @AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
        @DependsOn(SPRING_BEAN_NAME_CURRENT_WORKER)
        @ConditionalOnBean({Worker.class})
        public static class EtcdWorkerRegistryConfiguration {
            @Bean
            @ConditionalOnMissingBean
            public WorkerRegistry workerRegistry(@Value("${" + JobConstants.SCHEDULER_NAMESPACE + ":}") String namespace,
                                                 EtcdRegistryProperties config) {
                return new EtcdWorkerRegistry(namespace, config);
            }
        }
    }

}

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
import org.springframework.core.Ordered;

import static cn.ponfee.scheduler.core.base.JobConstants.SPRING_BEAN_NAME_CURRENT_SUPERVISOR;
import static cn.ponfee.scheduler.core.base.JobConstants.SPRING_BEAN_NAME_CURRENT_WORKER;

/**
 * Etcd server register & discovery configuration.
 *
 * @author Ponfee
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({EtcdServerRegistry.class})
public class EtcdServerRegistryConfigure {

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
                                                     EtcdProperties properties) {
            return new EtcdSupervisorRegistry(namespace, properties);
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
                                             EtcdProperties properties) {
            return new EtcdWorkerRegistry(namespace, properties);
        }
    }

}

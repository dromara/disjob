package cn.ponfee.scheduler.registry.zookeeper.configuration;

import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import cn.ponfee.scheduler.registry.WorkerRegistry;
import cn.ponfee.scheduler.registry.zookeeper.ZookeeperServerRegistry;
import cn.ponfee.scheduler.registry.zookeeper.ZookeeperSupervisorRegistry;
import cn.ponfee.scheduler.registry.zookeeper.ZookeeperWorkerRegistry;
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
 * Zookeeper server register & discovery configuration.
 *
 * @author Ponfee
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ZookeeperServerRegistry.class})
public class ZookeeperServerRegistryConfigure {

    /**
     * Configuration zookeeper supervisor registry.
     */
    @Configuration(proxyBeanMethods = false)
    @AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
    @DependsOn(SPRING_BEAN_NAME_CURRENT_SUPERVISOR)
    @ConditionalOnBean(Supervisor.class)
    public static class ZookeeperSupervisorRegistryConfiguration {
        @Bean
        @ConditionalOnMissingBean
        public SupervisorRegistry supervisorRegistry(@Value("${" + JobConstants.SCHEDULER_NAMESPACE + ":}") String namespace,
                                                     ZookeeperProperties props) {
            return new ZookeeperSupervisorRegistry(namespace, props);
        }
    }

    /**
     * Configuration zookeeper worker registry.
     */
    @Configuration(proxyBeanMethods = false)
    @AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
    @DependsOn(SPRING_BEAN_NAME_CURRENT_WORKER)
    @ConditionalOnBean(Worker.class)
    public static class ZookeeperWorkerRegistryConfiguration {
        @Bean
        @ConditionalOnMissingBean
        public WorkerRegistry workerRegistry(@Value("${" + JobConstants.SCHEDULER_NAMESPACE + ":}") String namespace,
                                             ZookeeperProperties props) {
            return new ZookeeperWorkerRegistry(namespace, props);
        }
    }

}

package cn.ponfee.scheduler.registry.nacos.configuration;

import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import cn.ponfee.scheduler.registry.WorkerRegistry;
import cn.ponfee.scheduler.registry.configuration.MarkServerRegistryAutoConfiguration;
import cn.ponfee.scheduler.registry.nacos.NacosSupervisorRegistry;
import cn.ponfee.scheduler.registry.nacos.NacosWorkerRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring autoconfiguration for nacos server registry
 *
 * @author Ponfee
 */
@EnableConfigurationProperties(NacosRegistryProperties.class)
public class NacosServerRegistryAutoConfiguration extends MarkServerRegistryAutoConfiguration {

    /**
     * Configuration nacos supervisor registry.
     */
    @ConditionalOnBean(Supervisor.class)
    @ConditionalOnMissingBean
    @Bean
    public SupervisorRegistry supervisorRegistry(NacosRegistryProperties config) {
        return new NacosSupervisorRegistry(config);
    }

    /**
     * Configuration nacos worker registry.
     */
    @ConditionalOnBean(Worker.class)
    @ConditionalOnMissingBean
    @Bean
    public WorkerRegistry workerRegistry(NacosRegistryProperties config) {
        return new NacosWorkerRegistry(config);
    }

}

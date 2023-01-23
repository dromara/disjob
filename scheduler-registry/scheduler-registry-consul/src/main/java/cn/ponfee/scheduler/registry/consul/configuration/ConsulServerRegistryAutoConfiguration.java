/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.registry.consul.configuration;

import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import cn.ponfee.scheduler.registry.WorkerRegistry;
import cn.ponfee.scheduler.registry.configuration.MarkServerRegistryAutoConfiguration;
import cn.ponfee.scheduler.registry.consul.ConsulSupervisorRegistry;
import cn.ponfee.scheduler.registry.consul.ConsulWorkerRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring autoconfiguration for consul server registry
 *
 * @author Ponfee
 */
@EnableConfigurationProperties(ConsulRegistryProperties.class)
public class ConsulServerRegistryAutoConfiguration extends MarkServerRegistryAutoConfiguration {

    /**
     * Configuration consul supervisor registry.
     */
    @ConditionalOnBean(Supervisor.class)
    @ConditionalOnMissingBean
    @Bean
    public SupervisorRegistry supervisorRegistry(ConsulRegistryProperties config) {
        return new ConsulSupervisorRegistry(config);
    }

    /**
     * Configuration consul worker registry.
     */
    @ConditionalOnBean(Worker.class)
    @ConditionalOnMissingBean
    @Bean
    public WorkerRegistry workerRegistry(ConsulRegistryProperties config) {
        return new ConsulWorkerRegistry(config);
    }

}

/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.registry.zookeeper.configuration;

import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import cn.ponfee.scheduler.registry.WorkerRegistry;
import cn.ponfee.scheduler.registry.configuration.BaseServerRegistryAutoConfiguration;
import cn.ponfee.scheduler.registry.zookeeper.ZookeeperSupervisorRegistry;
import cn.ponfee.scheduler.registry.zookeeper.ZookeeperWorkerRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring autoconfiguration for zookeeper server registry
 *
 * @author Ponfee
 */
@EnableConfigurationProperties(ZookeeperRegistryProperties.class)
public class ZookeeperServerRegistryAutoConfiguration extends BaseServerRegistryAutoConfiguration {

    /**
     * Configuration zookeeper supervisor registry.
     */
    @ConditionalOnBean(Supervisor.class)
    @ConditionalOnMissingBean
    @Bean
    public SupervisorRegistry supervisorRegistry(ZookeeperRegistryProperties config) {
        return new ZookeeperSupervisorRegistry(config);
    }

    /**
     * Configuration zookeeper worker registry.
     */
    @ConditionalOnBean(Worker.class)
    @ConditionalOnMissingBean
    @Bean
    public WorkerRegistry workerRegistry(ZookeeperRegistryProperties config) {
        return new ZookeeperWorkerRegistry(config);
    }

}

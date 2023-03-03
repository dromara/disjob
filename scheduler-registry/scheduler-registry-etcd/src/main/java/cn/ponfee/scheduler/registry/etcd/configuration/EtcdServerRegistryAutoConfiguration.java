/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.registry.etcd.configuration;

import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import cn.ponfee.scheduler.registry.WorkerRegistry;
import cn.ponfee.scheduler.registry.configuration.BaseServerRegistryAutoConfiguration;
import cn.ponfee.scheduler.registry.etcd.EtcdSupervisorRegistry;
import cn.ponfee.scheduler.registry.etcd.EtcdWorkerRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring autoconfiguration for etcd server registry
 *
 * @author Ponfee
 */
@EnableConfigurationProperties(EtcdRegistryProperties.class)
public class EtcdServerRegistryAutoConfiguration extends BaseServerRegistryAutoConfiguration {

    /**
     * Configuration etcd supervisor registry.
     */
    @ConditionalOnBean(Supervisor.class)
    @ConditionalOnMissingBean
    @Bean
    public SupervisorRegistry supervisorRegistry(EtcdRegistryProperties config) {
        return new EtcdSupervisorRegistry(config);
    }

    /**
     * Configuration etcd worker registry.
     */
    @ConditionalOnBean(Worker.class)
    @ConditionalOnMissingBean
    @Bean
    public WorkerRegistry workerRegistry(EtcdRegistryProperties config) {
        return new EtcdWorkerRegistry(config);
    }

}

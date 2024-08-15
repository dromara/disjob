/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.registry.etcd.configuration;

import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.registry.configuration.BaseServerRegistryAutoConfiguration;
import cn.ponfee.disjob.registry.etcd.EtcdSupervisorRegistry;
import cn.ponfee.disjob.registry.etcd.EtcdWorkerRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
    @ConditionalOnBean(Supervisor.Local.class)
    @Bean
    public SupervisorRegistry supervisorRegistry(EtcdRegistryProperties config) {
        return new EtcdSupervisorRegistry(config);
    }

    /**
     * Configuration etcd worker registry.
     */
    @ConditionalOnBean(Worker.Local.class)
    @Bean
    public WorkerRegistry workerRegistry(EtcdRegistryProperties config) {
        return new EtcdWorkerRegistry(config);
    }

}

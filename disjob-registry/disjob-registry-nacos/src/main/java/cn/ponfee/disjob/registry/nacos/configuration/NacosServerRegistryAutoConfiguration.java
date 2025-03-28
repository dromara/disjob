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

package cn.ponfee.disjob.registry.nacos.configuration;

import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.registry.configuration.BaseServerRegistryAutoConfiguration;
import cn.ponfee.disjob.registry.nacos.NacosSupervisorRegistry;
import cn.ponfee.disjob.registry.nacos.NacosWorkerRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * Spring autoconfiguration for nacos server registry
 *
 * @author Ponfee
 */
@EnableConfigurationProperties(NacosRegistryProperties.class)
public class NacosServerRegistryAutoConfiguration extends BaseServerRegistryAutoConfiguration {

    /**
     * Configuration nacos supervisor registry.
     */
    @ConditionalOnBean(Supervisor.Local.class)
    @Bean
    public SupervisorRegistry supervisorRegistry(NacosRegistryProperties config,
                                                 @Qualifier(JobConstants.SPRING_BEAN_NAME_REST_TEMPLATE) RestTemplate restTemplate) {
        return new NacosSupervisorRegistry(config, restTemplate);
    }

    /**
     * Configuration nacos worker registry.
     */
    @ConditionalOnBean(Worker.Local.class)
    @Bean
    public WorkerRegistry workerRegistry(NacosRegistryProperties config,
                                         @Qualifier(JobConstants.SPRING_BEAN_NAME_REST_TEMPLATE) RestTemplate restTemplate) {
        return new NacosWorkerRegistry(config, restTemplate);
    }

}

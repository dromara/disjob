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

package cn.ponfee.disjob.registry.nacos;

import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.registry.nacos.configuration.NacosRegistryProperties;
import org.springframework.web.client.RestTemplate;

/**
 * Registry supervisor based nacos.
 *
 * @author Ponfee
 */
public class NacosSupervisorRegistry extends NacosServerRegistry<Supervisor, Worker> implements SupervisorRegistry {

    public NacosSupervisorRegistry(NacosRegistryProperties config, RestTemplate restTemplate) {
        super(config, restTemplate);
    }

}

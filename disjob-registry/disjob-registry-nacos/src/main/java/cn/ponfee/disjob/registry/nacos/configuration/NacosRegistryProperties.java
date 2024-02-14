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
import cn.ponfee.disjob.registry.AbstractRegistryProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cglib.beans.BeanMap;

import java.util.Properties;

/**
 * Nacos registry configuration properties.
 *
 * @author Ponfee
 * @see com.alibaba.nacos.api.PropertyKeyConst
 */
@Getter
@Setter
@ConfigurationProperties(prefix = JobConstants.DISJOB_REGISTRY_KEY_PREFIX + ".nacos")
public class NacosRegistryProperties extends AbstractRegistryProperties {
    private static final long serialVersionUID = 2961908276104522907L;

    /**
     * Nacos server address
     */
    private String serverAddr = "localhost:8848";

    /**
     * Nacos server username
     */
    private String username = "nacos";

    /**
     * Nacos server password
     */
    private String password = "nacos";

    /**
     * Nacos server naming load cache at start
     */
    private String namingLoadCacheAtStart = "true";

    public Properties toProperties() {
        Properties properties = new Properties();
        BeanMap.create(this).forEach((k, v) -> {
            if (v != null) {
                properties.put(k, v);
            }
        });
        return properties;
    }

}

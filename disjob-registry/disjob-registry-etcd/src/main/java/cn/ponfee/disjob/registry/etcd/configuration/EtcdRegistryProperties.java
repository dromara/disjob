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

import cn.ponfee.disjob.registry.AbstractRegistryProperties;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;

/**
 * Etcd registry configuration properties.
 *
 * @author Ponfee
 */
@Getter
@Setter
@ConfigurationProperties(prefix = EtcdRegistryProperties.KEY_PREFIX)
public class EtcdRegistryProperties extends AbstractRegistryProperties {
    private static final long serialVersionUID = -7448688693230439783L;
    public static final String KEY_PREFIX = DISJOB_REGISTRY_KEY_PREFIX + ".etcd";

    /**
     * Server endpoints, multiple addresses separated by ","
     */
    private String endpoints = "localhost:2379";

    /**
     * Auth user
     */
    private String user;

    /**
     * Auth password
     */
    private String password;

    /**
     * Max inbound message size
     */
    private int maxInboundMessageSize = 100 * 1024 * 1024;

    /**
     * Request timeout milliseconds
     */
    private int requestTimeoutMs = 10 * 1000;

    /**
     * Session timeout milliseconds
     */
    private int sessionTimeoutMs = 60 * 1000;

    /**
     * Naming load cache at start
     */
    private String namingLoadCacheAtStart = "true";

    public String[] endpoints() {
        if (StringUtils.isBlank(endpoints)) {
            throw new IllegalArgumentException("Endpoints cannot be blank.");
        }

        return Arrays.stream(endpoints.split(","))
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .map(e -> e.contains("://") ? e : "http://" + e)
            .toArray(String[]::new);
    }

}

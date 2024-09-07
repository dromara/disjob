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

package cn.ponfee.disjob.registry.consul.configuration;

import cn.ponfee.disjob.registry.AbstractRegistryProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Consul registry configuration properties.
 *
 * @author Ponfee
 */
@Getter
@Setter
@ConfigurationProperties(prefix = ConsulRegistryProperties.KEY_PREFIX)
public class ConsulRegistryProperties extends AbstractRegistryProperties {
    private static final long serialVersionUID = -851364562631134942L;
    public static final String KEY_PREFIX = DISJOB_REGISTRY_KEY_PREFIX + ".consul";

    /**
     * Consul client host
     */
    private String host = "localhost";

    /**
     * Consul client port
     */
    private int port = 8500;

    /**
     * Consul token
     */
    private String token;

    /**
     * Check pass period seconds
     */
    private int checkPassPeriodSeconds = 3;

    /**
     * Check time to live
     */
    private String checkTtl = (checkPassPeriodSeconds * 3) + "s";

    /**
     * Check deregister critical timeout
     */
    private String checkDeregisterCriticalTimeout = "60m";

}

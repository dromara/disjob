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

package cn.ponfee.disjob.registry.database.configuration;

import cn.ponfee.disjob.registry.AbstractRegistryProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;

/**
 * Database registry configuration properties.
 *
 * @author Ponfee
 */
@Getter
@Setter
@ConfigurationProperties(prefix = DatabaseRegistryProperties.KEY_PREFIX)
public class DatabaseRegistryProperties extends AbstractRegistryProperties {
    private static final long serialVersionUID = -7144285250901660738L;
    public static final String KEY_PREFIX = DISJOB_REGISTRY_KEY_PREFIX + ".database";

    /**
     * Session timeout milliseconds
     */
    private long sessionTimeoutMs = 90 * 1000L;

    /**
     * Datasource configuration.
     */
    private DataSourceProperties datasource;

    @Getter
    @Setter
    public static class DataSourceProperties implements Serializable {
        private static final long serialVersionUID = 6995495113012945438L;

        private String driverClassName = "com.mysql.cj.jdbc.Driver";
        private String url;
        private String username;
        private String password;
        private Boolean autoCommit;
        private Boolean readOnly;

        private long connectionTimeout = 3000;
        private int minimumIdle = 1;
        private long idleTimeout = 600000;
        private int maximumPoolSize = 20;
        private long maxLifetime = 1800000;
        private String connectionTestQuery;
        private String poolName = "disjob_registry_database";
    }

}

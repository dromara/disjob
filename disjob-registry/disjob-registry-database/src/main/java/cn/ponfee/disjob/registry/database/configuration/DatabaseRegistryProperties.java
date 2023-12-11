/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.registry.database.configuration;

import cn.ponfee.disjob.core.base.JobConstants;
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
@ConfigurationProperties(prefix = JobConstants.DISJOB_REGISTRY_KEY_PREFIX + ".database")
public class DatabaseRegistryProperties extends AbstractRegistryProperties {
    private static final long serialVersionUID = -7144285250901660738L;

    /**
     * Session timeout milliseconds
     */
    private long sessionTimeoutMs = 30 * 1000L;

    /**
     * Datasource configuration.
     */
    private DataSourceProperties datasource;

    @Getter
    @Setter
    public static class DataSourceProperties implements Serializable {
        private static final long serialVersionUID = 6995495113012945438L;

        private String driverClassName = "com.mysql.cj.jdbc.Driver";
        private String jdbcUrl;
        private String username;
        private String password;
        private int minimumIdle = 1;
        private int maximumPoolSize = 20;
        private long connectionTimeout = 3000;
        private String poolName = "disjob_registry_database";
    }

}

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
    private long sessionTimeoutMs = 30 * 1000;

    /**
     * Registry period milliseconds
     */
    private long registryPeriodMs = 10 * 1000;

}

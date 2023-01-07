/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.registry.etcd.configuration;

import cn.ponfee.scheduler.core.base.JobConstants;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;

/**
 * Etcd configuration properties.
 *
 * @author Ponfee
 */
@ConfigurationProperties(prefix = JobConstants.SCHEDULER_KEY_PREFIX + ".etcd")
@Data
public class EtcdProperties {

    /**
     * Server endpoints, multiple addresses separated by ","
     */
    private String endpoints = "localhost:2379";

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

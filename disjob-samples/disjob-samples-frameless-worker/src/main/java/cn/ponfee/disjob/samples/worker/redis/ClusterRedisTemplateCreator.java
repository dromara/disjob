/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.samples.worker.redis;

import lombok.experimental.SuperBuilder;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConfiguration;

import java.util.Arrays;

/**
 * Creates cluster spring redis template
 *
 * @author Ponfee
 */
@SuperBuilder(toBuilder = true)
public class ClusterRedisTemplateCreator extends AbstractRedisTemplateCreator {

    private String clusterNodes;
    private Integer clusterMaxRedirects;

    @Override
    protected RedisConfiguration createRedisConfiguration() {
        RedisClusterConfiguration configuration = new RedisClusterConfiguration(
            Arrays.asList(clusterNodes.split(","))
        );
        if (clusterMaxRedirects != null) {
            configuration.setMaxRedirects(clusterMaxRedirects);
        }
        configuration.setUsername(super.username);
        configuration.setPassword(super.password);
        return configuration;
    }

}

/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.samples.worker.redis;

import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates sentinel spring redis template
 *
 * @author Ponfee
 */
@SuperBuilder(toBuilder = true)
public class SentinelRedisTemplateCreator extends AbstractRedisTemplateCreator {

    private String sentinelMaster;
    private String sentinelNodes;
    private String sentinelUsername;
    private String sentinelPassword;

    @Override
    protected RedisConfiguration createRedisConfiguration() {
        RedisSentinelConfiguration configuration = new RedisSentinelConfiguration();
        configuration.setDatabase(super.database);
        configuration.setUsername(super.username);
        configuration.setPassword(super.password);
        configuration.setMaster(sentinelMaster);

        List<RedisNode> redisNodes = Arrays.stream(sentinelNodes.split(","))
            .filter(StringUtils::isNotBlank)
            .map(RedisNode::fromString)
            .collect(Collectors.toList());
        configuration.setSentinels(redisNodes);

        // Sentinel username/password
        configuration.setSentinelUsername(sentinelUsername);
        if (StringUtils.isNotEmpty(sentinelPassword)) {
            configuration.setSentinelPassword(RedisPassword.of(sentinelPassword));
        }

        return configuration;
    }

}

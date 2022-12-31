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
@SuperBuilder
public class SentinelRedisTemplateCreator extends AbstractRedisTemplateCreator {

    private String sentinelMaster;
    private String sentinelNodes;
    private String sentinelUsername;
    private String sentinelPassword;

    @Override
    protected RedisConfiguration createRedisConfiguration() {
        RedisSentinelConfiguration configuration = new RedisSentinelConfiguration();
        configuration.setDatabase(database);
        configuration.setUsername(username);
        configuration.setPassword(password);
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

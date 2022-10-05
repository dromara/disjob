package cn.ponfee.scheduler.worker.samples.redis;

import lombok.experimental.SuperBuilder;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates sentinel spring redis template
 *
 * @author Ponfee
 */
@SuperBuilder
public class SentinelRedisTemplateCreator extends AbstractRedisTemplateCreator {

    private String sentinelMaster;
    private String sentinelNodes;

    @Override
    protected RedisConfiguration createRedisConfiguration() {
        RedisSentinelConfiguration configuration = new RedisSentinelConfiguration();
        configuration.setDatabase(database);
        configuration.setUsername(username);
        configuration.setPassword(password);
        configuration.setMaster(sentinelMaster);
        configuration.setSentinels(createSentinels(sentinelNodes.split(",")));
        return configuration;
    }

    private static List<RedisNode> createSentinels(String[] array) {
        List<RedisNode> nodes = new ArrayList<>(array.length);
        for (String node : array) {
            try {
                String[] parts = StringUtils.split(node, ":");
                Assert.state(parts.length == 2, "Must be defined as 'host:port'");
                nodes.add(new RedisNode(parts[0], Integer.parseInt(parts[1])));
            } catch (Exception e) {
                throw new IllegalStateException("Invalid redis sentinel property '" + node + "'", e);
            }
        }
        Assert.notEmpty(nodes, "Not config any node sentinel.");
        return nodes;
    }
}

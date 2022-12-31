package cn.ponfee.scheduler.samples.worker.redis;

import lombok.experimental.SuperBuilder;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConfiguration;

import java.util.Arrays;

/**
 * Creates cluster spring redis template
 *
 * @author Ponfee
 */
@SuperBuilder
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
        configuration.setUsername(username);
        configuration.setPassword(password);
        return configuration;
    }

}

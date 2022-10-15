package cn.ponfee.scheduler.samples.worker.redis;

import lombok.experimental.SuperBuilder;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;

/**
 * Creates standalone spring redis template
 *
 * @author Ponfee
 */
@SuperBuilder
public class StandaloneRedisTemplateCreator extends AbstractRedisTemplateCreator {

    private String host;
    private int port;

    @Override
    protected RedisConfiguration createRedisConfiguration() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setDatabase(database);
        configuration.setUsername(username);
        configuration.setPassword(password);
        configuration.setHostName(host);
        configuration.setPort(port);
        return configuration;
    }

}

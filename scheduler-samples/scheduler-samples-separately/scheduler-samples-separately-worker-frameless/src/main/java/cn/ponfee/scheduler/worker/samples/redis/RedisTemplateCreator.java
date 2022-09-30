package cn.ponfee.scheduler.worker.samples.redis;

import cn.ponfee.scheduler.common.util.Jsons;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import lombok.Builder;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates Spring redis template
 *
 * @author Ponfee
 */
@Builder
public class RedisTemplateCreator {

    private int database;
    private String password;
    private int connectTimeout;
    private int timeout;
    private String sentinelMaster;
    private String sentinelNodes;

    private int maxActive;
    private int maxIdle;
    private int minIdle;
    private int maxWait;
    private int shutdownTimeout;

    public RedisTemplate createRedisTemplate() {
        Jackson2JsonRedisSerializer<Object> valueSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        valueSerializer.setObjectMapper(Jsons.createObjectMapper(JsonInclude.Include.NON_NULL));

        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(createRedisConnectionFactory());
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setValueSerializer(valueSerializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    public StringRedisTemplate createStringRedisTemplate() {
        return new StringRedisTemplate(createRedisConnectionFactory());
    }

    private RedisConnectionFactory createRedisConnectionFactory() {
        // ----------------------------------------basic config
        //RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        RedisSentinelConfiguration configuration = new RedisSentinelConfiguration();
        configuration.setDatabase(database);
        configuration.setPassword(password);
        configuration.setMaster(sentinelMaster);
        configuration.setSentinels(createSentinels(sentinelNodes.split(",")));

        // ----------------------------------------pool config
        GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();
        genericObjectPoolConfig.setMaxIdle(maxIdle);
        genericObjectPoolConfig.setMinIdle(minIdle);
        genericObjectPoolConfig.setMaxTotal(maxActive);
        genericObjectPoolConfig.setMaxWait(Duration.ofMillis(maxWait));

        // ----------------------------------------lettuce config
        ClientOptions clientOptions = ClientOptions.builder()
            .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofMillis(connectTimeout)).build())
            .build();

        LettuceClientConfiguration lettuceClientConfiguration = LettucePoolingClientConfiguration.builder()
            .poolConfig(genericObjectPoolConfig)
            .commandTimeout(Duration.ofMillis(timeout))
            .shutdownTimeout(Duration.ofMillis(shutdownTimeout))
            .clientOptions(clientOptions)
            .build();
        LettuceConnectionFactory redisConnectionFactory = new LettuceConnectionFactory(configuration, lettuceClientConfiguration);
        redisConnectionFactory.afterPropertiesSet();
        return redisConnectionFactory;
    }

    private static List<RedisNode> createSentinels(String[] array) {
        List<RedisNode> nodes = new ArrayList<>(array.length);
        for (String node : array) {
            try {
                String[] parts = StringUtils.split(node, ":");
                Assert.state(parts.length == 2, "Must be defined as 'host:port'");
                nodes.add(new RedisNode(parts[0], Integer.parseInt(parts[1])));
            } catch (RuntimeException ex) {
                throw new IllegalStateException("Invalid redis sentinel property '" + node + "'", ex);
            }
        }
        return nodes;
    }

}

package cn.ponfee.scheduler.samples.worker.redis;

import cn.ponfee.scheduler.common.util.Jsons;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;

/**
 * Abstract spring redis template creator
 *
 * @author Ponfee
 */
@SuperBuilder
public abstract class AbstractRedisTemplateCreator {

    protected int database;
    protected String username;
    protected String password;

    private int connectTimeout;
    private int timeout;

    private int maxActive;
    private int maxIdle;
    private int minIdle;
    private int maxWait;
    private int shutdownTimeout;

    public final RedisTemplateWrapper create() {
        RedisConnectionFactory redisConnectionFactory = createRedisConnectionFactory();

        RedisTemplate<Object, Object> normalRedisTemplate = new RedisTemplate<>();
        Jackson2JsonRedisSerializer<Object> valueSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        valueSerializer.setObjectMapper(Jsons.createObjectMapper(JsonInclude.Include.NON_NULL));
        normalRedisTemplate.setConnectionFactory(redisConnectionFactory);
        normalRedisTemplate.setKeySerializer(RedisSerializer.string());
        normalRedisTemplate.setValueSerializer(valueSerializer);
        normalRedisTemplate.afterPropertiesSet();

        StringRedisTemplate stringRedisTemplate = new StringRedisTemplate(redisConnectionFactory);

        return new RedisTemplateWrapper(redisConnectionFactory, normalRedisTemplate, stringRedisTemplate);
    }

    protected abstract RedisConfiguration createRedisConfiguration();

    private RedisConnectionFactory createRedisConnectionFactory() {
        // ----------------------------------------basic config
        RedisConfiguration configuration = createRedisConfiguration();

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

        // ----------------------------------------crate lettuce connection factory
        LettuceConnectionFactory redisConnectionFactory = new LettuceConnectionFactory(configuration, lettuceClientConfiguration);
        redisConnectionFactory.afterPropertiesSet();
        return redisConnectionFactory;
    }

    @Data
    public static class RedisTemplateWrapper {
        private final RedisConnectionFactory redisConnectionFactory;
        private final RedisTemplate<Object, Object> normalRedisTemplate;
        private final StringRedisTemplate stringRedisTemplate;
    }

}

/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.samples.worker.redis;

import cn.ponfee.disjob.common.spring.YamlProperties;
import cn.ponfee.disjob.common.util.Jsons;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import lombok.AllArgsConstructor;
import lombok.Getter;
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

    private ObjectMapper objectMapper;

    public final RedisTemplateWrapper create() {
        if (this.objectMapper == null) {
            this.objectMapper = Jsons.createObjectMapper(JsonInclude.Include.NON_NULL);
        }

        RedisConnectionFactory redisConnectionFactory = createRedisConnectionFactory();

        RedisTemplate<Object, Object> normalRedisTemplate = new RedisTemplate<>();
        Jackson2JsonRedisSerializer<Object> valueSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        valueSerializer.setObjectMapper(objectMapper);
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
        GenericObjectPoolConfig<?> genericObjectPoolConfig = new GenericObjectPoolConfig<>();
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

    @Getter
    @AllArgsConstructor
    public static class RedisTemplateWrapper {
        private final RedisConnectionFactory redisConnectionFactory;
        private final RedisTemplate<Object, Object> normalRedisTemplate;
        private final StringRedisTemplate stringRedisTemplate;
    }

    public static RedisTemplateWrapper create(String prefix, YamlProperties props, ObjectMapper objectMapper) {
        AbstractRedisTemplateCreatorBuilder<?, ?> builder;
        if (props.hasKey(prefix + "host")) {
            // Creates standalone redis template
            builder = StandaloneRedisTemplateCreator.builder()
                .host(props.getRequiredString(prefix + "host"))
                .port(props.getRequiredInt(prefix + "port"));
        } else if (props.hasKey(prefix + "sentinel.master")) {
            // Creates sentinel redis template
            builder = SentinelRedisTemplateCreator.builder()
                .sentinelMaster(props.getRequiredString(prefix + "sentinel.master"))
                .sentinelNodes(props.getRequiredString(prefix + "sentinel.nodes"))
                .sentinelUsername(props.getString(prefix + "sentinel.username"))
                .sentinelPassword(props.getString(prefix + "sentinel.password"));
        } else if (props.hasKey(prefix + "cluster.nodes")) {
            // Creates cluster redis template
            builder = ClusterRedisTemplateCreator.builder()
                .clusterNodes(props.getRequiredString(prefix + "cluster.nodes"))
                .clusterMaxRedirects(props.getInt(prefix + "cluster.max-redirects"));
        } else {
            throw new IllegalArgumentException("Invalid redis configuration: " + Jsons.toJson(props));
        }

        return builder.database(props.getInt(prefix + "database", 0))
            .username(props.getString(prefix + "username"))
            .password(props.getString(prefix + "password"))
            .connectTimeout(props.getInt(prefix + "connect-timeout", 1000))
            .timeout(props.getInt(prefix + "timeout", 2000))
            .maxActive(props.getInt(prefix + "lettuce.pool.max-active", 50))
            .maxIdle(props.getInt(prefix + "lettuce.pool.max-idle", 8))
            .minIdle(props.getInt(prefix + "lettuce.pool.min-idle", 0))
            .maxWait(props.getInt(prefix + "lettuce.pool.max-wait", 2000))
            .shutdownTimeout(props.getInt("lettuce.shutdown-timeout", 2000))
            .objectMapper(objectMapper)
            .build()
            .create();
    }

}

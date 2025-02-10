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

package cn.ponfee.disjob.common.spring;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration.LettuceClientConfigurationBuilder;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * RedisTemplate factory
 *
 * @author Ponfee
 */
public class RedisTemplateFactory implements Closeable {

    private final RedisProperties properties;
    private final ClientResources clientResources;
    private final LettuceConnectionFactory redisConnectionFactory;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public RedisTemplateFactory(RedisProperties properties) {
        this(properties, DefaultClientResources.builder().build(), null);
    }

    public RedisTemplateFactory(RedisProperties properties,
                                ClientResources clientResources,
                                ObjectProvider<LettuceClientConfigurationBuilderCustomizer> builderCustomizers) {
        if (properties.getClientType() != null && properties.getClientType() != RedisProperties.ClientType.LETTUCE) {
            throw new IllegalArgumentException("Unsupported redis client type: " + properties.getClientType());
        }

        this.properties = properties;
        this.clientResources = clientResources;
        this.redisConnectionFactory = createConnectionFactory(builderCustomizers);
        this.redisTemplate = createRedisTemplate(redisConnectionFactory);
        this.stringRedisTemplate = new StringRedisTemplate(redisConnectionFactory);
    }

    public LettuceConnectionFactory getRedisConnectionFactory() {
        return redisConnectionFactory;
    }

    public RedisTemplate<Object, Object> getRedisTemplate() {
        return redisTemplate;
    }

    public StringRedisTemplate getStringRedisTemplate() {
        return stringRedisTemplate;
    }

    @Override
    public void close() throws IOException {
        clientResources.shutdown();
    }

    // --------------------------------------------------------------------private methods

    private LettuceConnectionFactory createConnectionFactory(ObjectProvider<LettuceClientConfigurationBuilderCustomizer> builderCustomizers) {
        LettuceClientConfigurationBuilder builder = createBuilder();
        builder.clientOptions(createClientOptions());
        builder.clientResources(clientResources);
        if (builderCustomizers != null) {
            builderCustomizers.orderedStream().forEach(customizer -> customizer.customize(builder));
        }

        LettuceClientConfiguration clientConfiguration = builder.build();
        LettuceConnectionFactory connectionFactory;
        RedisSentinelConfiguration sentinelConfiguration;
        RedisClusterConfiguration clusterConfiguration;
        if ((sentinelConfiguration = getSentinelConfiguration()) != null) {
            connectionFactory = new LettuceConnectionFactory(sentinelConfiguration, clientConfiguration);
        } else if ((clusterConfiguration = getClusterConfiguration()) != null) {
            connectionFactory = new LettuceConnectionFactory(clusterConfiguration, clientConfiguration);
        } else {
            connectionFactory = new LettuceConnectionFactory(getStandaloneConfiguration(), clientConfiguration);
        }
        connectionFactory.afterPropertiesSet();
        return connectionFactory;
    }

    private LettuceClientConfigurationBuilder createBuilder() {
        RedisProperties.Pool pool = properties.getLettuce().getPool();
        LettuceClientConfigurationBuilder builder;
        if (Boolean.FALSE.equals(pool.getEnabled())) {
            builder = LettuceClientConfiguration.builder();
        } else {
            GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
            poolConfig.setMaxTotal(pool.getMaxActive());
            poolConfig.setMaxIdle(pool.getMaxIdle());
            poolConfig.setMinIdle(pool.getMinIdle());
            if (pool.getTimeBetweenEvictionRuns() != null) {
                poolConfig.setTimeBetweenEvictionRuns(pool.getTimeBetweenEvictionRuns());
            }
            if (pool.getMaxWait() != null) {
                poolConfig.setMaxWait(pool.getMaxWait());
            }
            builder = LettucePoolingClientConfiguration.builder().poolConfig(poolConfig);
        }

        // apply properties
        if (properties.isSsl()) {
            builder.useSsl();
        }
        if (properties.getTimeout() != null) {
            builder.commandTimeout(properties.getTimeout());
        }
        if (properties.getLettuce() != null) {
            RedisProperties.Lettuce lettuce = properties.getLettuce();
            if (lettuce.getShutdownTimeout() != null && !lettuce.getShutdownTimeout().isZero()) {
                builder.shutdownTimeout(properties.getLettuce().getShutdownTimeout());
            }
        }
        if (StringUtils.hasText(properties.getClientName())) {
            builder.clientName(properties.getClientName());
        }

        String url = properties.getUrl();
        if (StringUtils.hasText(url) && parseUrl(url).isUseSsl()) {
            builder.useSsl();
        }

        return builder;
    }

    private ClientOptions createClientOptions() {
        ClientOptions.Builder clientOptionsBuilder;
        if (properties.getCluster() == null) {
            clientOptionsBuilder = ClientOptions.builder();
        } else {
            RedisProperties.Lettuce.Cluster.Refresh refreshProperties = properties.getLettuce().getCluster().getRefresh();
            ClusterTopologyRefreshOptions.Builder refreshBuilder = ClusterTopologyRefreshOptions.builder()
                .dynamicRefreshSources(refreshProperties.isDynamicRefreshSources());
            if (refreshProperties.getPeriod() != null) {
                refreshBuilder.enablePeriodicRefresh(refreshProperties.getPeriod());
            }
            if (refreshProperties.isAdaptive()) {
                refreshBuilder.enableAllAdaptiveRefreshTriggers();
            }
            clientOptionsBuilder = ClusterClientOptions.builder().topologyRefreshOptions(refreshBuilder.build());
        }

        Duration connectTimeout = properties.getConnectTimeout();
        if (connectTimeout != null) {
            clientOptionsBuilder.socketOptions(SocketOptions.builder().connectTimeout(connectTimeout).build());
        }
        return clientOptionsBuilder.timeoutOptions(TimeoutOptions.enabled()).build();
    }

    private RedisSentinelConfiguration getSentinelConfiguration() {
        RedisProperties.Sentinel sentinelProperties = properties.getSentinel();
        if (sentinelProperties == null) {
            return null;
        }

        RedisSentinelConfiguration config = new RedisSentinelConfiguration();
        config.master(sentinelProperties.getMaster());
        config.setSentinels(createSentinels(sentinelProperties));
        config.setUsername(properties.getUsername());
        if (properties.getPassword() != null) {
            config.setPassword(RedisPassword.of(properties.getPassword()));
        }
        config.setSentinelUsername(sentinelProperties.getUsername());
        if (sentinelProperties.getPassword() != null) {
            config.setSentinelPassword(RedisPassword.of(sentinelProperties.getPassword()));
        }
        config.setDatabase(properties.getDatabase());
        return config;
    }

    private RedisClusterConfiguration getClusterConfiguration() {
        RedisProperties.Cluster clusterProperties = properties.getCluster();
        if (clusterProperties == null) {
            return null;
        }

        RedisClusterConfiguration config = new RedisClusterConfiguration(clusterProperties.getNodes());
        if (clusterProperties.getMaxRedirects() != null) {
            config.setMaxRedirects(clusterProperties.getMaxRedirects());
        }
        config.setUsername(properties.getUsername());
        if (properties.getPassword() != null) {
            config.setPassword(RedisPassword.of(properties.getPassword()));
        }
        return config;
    }

    private RedisStandaloneConfiguration getStandaloneConfiguration() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        if (StringUtils.hasText(properties.getUrl())) {
            ConnectionInfo connectionInfo = parseUrl(properties.getUrl());
            config.setHostName(connectionInfo.getHostName());
            config.setPort(connectionInfo.getPort());
            config.setUsername(connectionInfo.getUsername());
            config.setPassword(RedisPassword.of(connectionInfo.getPassword()));
        } else {
            config.setHostName(properties.getHost());
            config.setPort(properties.getPort());
            config.setUsername(properties.getUsername());
            config.setPassword(RedisPassword.of(properties.getPassword()));
        }
        config.setDatabase(properties.getDatabase());
        return config;
    }

    // --------------------------------------------------------------------private static methods/class

    private static RedisTemplate<Object, Object> createRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    private static ConnectionInfo parseUrl(String url) {
        String[] schemes = {"redis", "rediss"};
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (!ArrayUtils.contains(schemes, scheme)) {
                throw new UnsupportedOperationException(url);
            }
            boolean useSsl = schemes[1].equals(scheme);
            String username = null;
            String password = null;
            if (uri.getUserInfo() != null) {
                String candidate = uri.getUserInfo();
                int index = candidate.indexOf(':');
                if (index >= 0) {
                    username = candidate.substring(0, index);
                    password = candidate.substring(index + 1);
                } else {
                    password = candidate;
                }
            }
            return new ConnectionInfo(uri, useSsl, username, password);
        } catch (URISyntaxException ex) {
            throw new UnsupportedOperationException(url, ex);
        }
    }

    private static List<RedisNode> createSentinels(RedisProperties.Sentinel sentinel) {
        List<RedisNode> nodes = new ArrayList<>();
        for (String node : sentinel.getNodes()) {
            try {
                nodes.add(RedisNode.fromString(node));
            } catch (RuntimeException ex) {
                throw new IllegalStateException("Invalid redis sentinel property '" + node + "'", ex);
            }
        }
        return nodes;
    }

    @Getter
    static class ConnectionInfo {
        private final URI uri;
        private final boolean useSsl;
        private final String username;
        private final String password;

        ConnectionInfo(URI uri, boolean useSsl, String username, String password) {
            this.uri = uri;
            this.useSsl = useSsl;
            this.username = username;
            this.password = password;
        }

        String getHostName() {
            return this.uri.getHost();
        }

        int getPort() {
            return this.uri.getPort();
        }
    }

}

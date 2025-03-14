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

package cn.ponfee.disjob.registry.redis;

import cn.ponfee.disjob.common.base.RetryTemplate;
import cn.ponfee.disjob.common.concurrent.LoopThread;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingSupplier;
import cn.ponfee.disjob.common.spring.RedisTemplateUtils;
import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.core.enums.RegistryEventType;
import cn.ponfee.disjob.registry.ServerRegistry;
import cn.ponfee.disjob.registry.redis.configuration.RedisRegistryProperties;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static cn.ponfee.disjob.common.base.Symbol.Str.COLON;

/**
 * Registry server based redis.
 * <p><a href="https://english.stackexchange.com/questions/25931/unregister-vs-deregister">unregister-vs-deregister</a>
 *
 * @author Ponfee
 */
public abstract class RedisServerRegistry<R extends Server, D extends Server> extends ServerRegistry<R, D> {

    private static final String CHANNEL = "channel";

    private static final RedisScript<Void> REGISTRY_SCRIPT = RedisScript.of(
        "local score  = ARGV[1];                        \n" +
        "local expire = ARGV[2];                        \n" +
        "local length = #ARGV;                          \n" +
        "for i = 3,length do                            \n" +
        "  redis.call('zadd', KEYS[1], score, ARGV[i]); \n" +
        "end                                            \n" +
        "redis.call('pexpire', KEYS[1], expire);        \n" ,
        Void.class
    );

    @SuppressWarnings("rawtypes")
    private static final RedisScript<List> QUERY_SCRIPT = RedisScript.of(
        "redis.call('zremrangebyscore', KEYS[1], '-inf', ARGV[1]);          \n" +
        "local ret = redis.call('zrangebyscore', KEYS[1], ARGV[1], '+inf'); \n" +
        "redis.call('pexpire', KEYS[1], ARGV[2]);                           \n" +
        "return ret;                                                        \n" ,
        List.class
    );

    private static final String REDIS_KEY_TTL_MILLIS = Long.toString(TimeUnit.DAYS.toMillis(30));

    /**
     * Registry publish redis message channel
     */
    private final String registryChannel;

    /**
     * Spring string redis template
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Session timeout milliseconds
     */
    private final long sessionTimeoutMs;

    // -------------------------------------------------Registry

    private final LoopThread registerHeartbeatThread;
    private final List<String> registryRedisKey;

    // -------------------------------------------------Discovery

    private final LoopThread discoverHeartbeatThread;
    private final List<String> discoveryRedisKey;

    // -------------------------------------------------Subscribe

    private final RedisMessageListenerContainer redisMessageListenerContainer;

    protected RedisServerRegistry(RedisRegistryProperties config, RestTemplate restTemplate, StringRedisTemplate stringRedisTemplate) {
        super(config, restTemplate, ':');
        this.registryChannel = registryRootPath + separator + CHANNEL;
        this.stringRedisTemplate = stringRedisTemplate;
        this.sessionTimeoutMs = config.getSessionTimeoutMs();
        this.registryRedisKey = Collections.singletonList(registryRootPath);
        this.discoveryRedisKey = Collections.singletonList(discoveryRootPath);

        long periodMs = sessionTimeoutMs / 3;

        // -------------------------------------------------registry
        ThrowingRunnable<?> registerAction = () -> RetryTemplate.execute(() -> registerServers(registered), 3, 1000);
        this.registerHeartbeatThread = LoopThread.createStarted("redis_register_heartbeat", periodMs, periodMs, registerAction);

        // -------------------------------------------------discovery
        this.discoverHeartbeatThread = LoopThread.createStarted("redis_discover_heartbeat", periodMs, periodMs, this::discoverServers);

        // redis pub/sub
        this.redisMessageListenerContainer = RedisTemplateUtils.createRedisMessageListenerContainer(
            stringRedisTemplate,
            discoveryRootPath + separator + CHANNEL,
            new SyncTaskExecutor(),
            this,
            "handleMessage"
        );

        log.info("Redis server registry initialized: {}", RedisTemplateUtils.getServerInfo(stringRedisTemplate));
    }

    @Override
    public boolean isConnected() {
        Boolean result = stringRedisTemplate.execute((RedisCallback<Boolean>) conn -> !conn.isClosed());
        return Boolean.TRUE.equals(result);
    }

    // ------------------------------------------------------------------Registry

    @Override
    public final void register(R server) {
        if (state.isStopped()) {
            return;
        }

        registerServers(Collections.singleton(server));
        registered.add(server);
        publishServerEvent(RegistryEventType.REGISTER, server);
        log.info("Server registered: {}, {}", registryRole, server);
    }

    @Override
    public final void deregister(R server) {
        try {
            registered.remove(server);
            ThrowingSupplier.doCaught(() -> stringRedisTemplate.opsForZSet().remove(registryRootPath, server.serialize()));
            publishServerEvent(RegistryEventType.DEREGISTER, server);
            log.info("Redis server deregister success: {}", server);
        } catch (Throwable t) {
            log.error("Redis server deregister error: " + server, t);
        }
    }

    @Override
    public List<R> getRegisteredServers() {
        return deserializeServers(getServers(registryRedisKey), registryRole);
    }

    // ------------------------------------------------------------------Discovery

    @Override
    public void discoverServers() throws Throwable {
        RetryTemplate.execute(() -> refreshDiscoveryServers(getServers(discoveryRedisKey)), 3, 1000L);
    }

    // ------------------------------------------------------------------Close

    @PreDestroy
    @Override
    public void close() {
        if (!state.stop()) {
            return;
        }
        registerHeartbeatThread.terminate();
        registered.forEach(this::deregister);
        ThrowingRunnable.doCaught(redisMessageListenerContainer::stop);
        discoverHeartbeatThread.terminate();
        super.close();
    }

    // ------------------------------------------------------------------Subscribe

    /**
     * For redis message subscribe invoke.
     *
     * @param message the message
     * @param channel the channel
     */
    public void handleMessage(String message, String channel) {
        try {
            log.info("Handle message begin: {}, {}", message, channel);
            String[] array = message.split(COLON, 2);
            RegistryEventType eventType = RegistryEventType.valueOf(array[0]);
            D server = deserializeServer(array[1], discoveryRole);
            subscribeServerEvent(eventType, server);
        } catch (Throwable t) {
            log.error("Handle message error: " + message + ", " + channel, t);
        }
    }

    @Override
    protected void publishServerEvent(RegistryEventType eventType, R server) {
        log.info("Publish server event: {}, {}", eventType, server);
        String message = eventType.name() + COLON + server.serialize();
        ThrowingRunnable.doCaught(() -> stringRedisTemplate.convertAndSend(registryChannel, message));
    }

    // ------------------------------------------------------------------private methods

    /**
     * <pre>
     * Also use pipelined
     *
     * {@code
     *    Set<ZSetOperations.TypedTuple<String>> tuples = servers
     *        .stream()
     *        .map(e -> ZSetOperations.TypedTuple.of(e.serialize(), score))
     *        .collect(Collectors.toSet());
     *
     *    stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
     *        @Override
     *        public Void execute(RedisOperations operations) {
     *            operations.opsForZSet().add(registryRootPath, tuples);
     *            operations.expire(registryRootPath, REDIS_KEY_TTL_MILLIS, TimeUnit.MILLISECONDS);
     *            // in pipelined, must return null
     *            return null;
     *        }
     *    });
     * }
     * </pre>
     *
     * @param servers the registry servers
     */
    private void registerServers(Set<R> servers) {
        if (CollectionUtils.isEmpty(servers)) {
            return;
        }

        int i = 0;
        Object[] args = new Object[servers.size() + 2];
        args[i++] = Long.toString(System.currentTimeMillis() + sessionTimeoutMs);
        args[i++] = REDIS_KEY_TTL_MILLIS;
        for (R server : servers) {
            args[i++] = server.serialize();
        }
        stringRedisTemplate.execute(REGISTRY_SCRIPT, registryRedisKey, args);
    }

    @SuppressWarnings("unchecked")
    private List<String> getServers(List<String> serverRoleKey) {
        String baseScore = Long.toString(System.currentTimeMillis());
        return stringRedisTemplate.execute(QUERY_SCRIPT, serverRoleKey, baseScore, REDIS_KEY_TTL_MILLIS);
    }

}

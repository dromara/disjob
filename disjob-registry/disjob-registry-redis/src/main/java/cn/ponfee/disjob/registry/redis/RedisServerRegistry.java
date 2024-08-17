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
import cn.ponfee.disjob.common.base.TextTokenizer;
import cn.ponfee.disjob.common.concurrent.LoopThread;
import cn.ponfee.disjob.common.concurrent.NamedThreadFactory;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.concurrent.Threads;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingSupplier;
import cn.ponfee.disjob.common.spring.RedisTemplateUtils;
import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.registry.RegistryEventType;
import cn.ponfee.disjob.registry.ServerRegistry;
import cn.ponfee.disjob.registry.redis.configuration.RedisRegistryProperties;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static cn.ponfee.disjob.common.base.Symbol.Str.COLON;

/**
 * Registry server based redis.
 * <p><a href="https://english.stackexchange.com/questions/25931/unregister-vs-deregister">unregister-vs-deregister</a>
 *
 * @author Ponfee
 */
public abstract class RedisServerRegistry<R extends Server, D extends Server> extends ServerRegistry<R, D> {

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

    private static final String REDIS_KEY_TTL_MILLIS = Long.toString(30L * 86400 * 1000);
    private static final String CHANNEL = "channel";

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

    /**
     * Period milliseconds
     */
    private final long periodMs;

    // -------------------------------------------------Registry

    private final LoopThread registerHeartbeatThread;
    private final List<String> registryRedisKey;

    // -------------------------------------------------Discovery

    private final LoopThread discoverHeartbeatThread;
    private final ThreadPoolExecutor redisSubscribeExecutor;
    private final Lock asyncRefreshLock = new ReentrantLock();
    private final List<String> discoveryRedisKey;
    private volatile long nextDiscoverTimeMillis = 0;

    // -------------------------------------------------Subscribe

    private final RedisMessageListenerContainer redisMessageListenerContainer;

    protected RedisServerRegistry(StringRedisTemplate stringRedisTemplate,
                                  RedisRegistryProperties config) {
        super(config, ':');
        this.registryChannel = registryRootPath + separator + CHANNEL;
        this.stringRedisTemplate = stringRedisTemplate;
        this.sessionTimeoutMs = config.getSessionTimeoutMs();
        this.periodMs = config.getSessionTimeoutMs() / 3;
        this.registryRedisKey = Collections.singletonList(registryRootPath);
        this.discoveryRedisKey = Collections.singletonList(discoveryRootPath);

        // -------------------------------------------------registry
        ThrowingRunnable<?> registerAction = () -> RetryTemplate.execute(() -> doRegisterServers(registered), 3, 1000);
        this.registerHeartbeatThread = LoopThread.createStarted("redis_register_heartbeat", periodMs, periodMs, registerAction);

        // -------------------------------------------------discovery
        ThrowingRunnable<?> discoverAction = () -> { if (requireDiscoverServers()) { tryDiscoverServers(); } };
        this.discoverHeartbeatThread = LoopThread.createStarted("redis_discover_heartbeat", periodMs, periodMs, discoverAction);

        this.redisSubscribeExecutor = ThreadPoolExecutors.builder()
            .corePoolSize(1)
            .maximumPoolSize(1)
            .workQueue(new ArrayBlockingQueue<>(1))
            .keepAliveTimeSeconds(600)
            .rejectedHandler(ThreadPoolExecutors.DISCARD)
            .threadFactory(NamedThreadFactory.builder().prefix("redis_async_subscribe").priority(Thread.MAX_PRIORITY).uncaughtExceptionHandler(log).build())
            .build();

        // redis pub/sub
        String discoveryChannel = discoveryRootPath + separator + CHANNEL;
        this.redisMessageListenerContainer = RedisTemplateUtils.createRedisMessageListenerContainer(
            stringRedisTemplate, discoveryChannel, redisSubscribeExecutor, this, "handleMessage");

        try {
            doDiscoverServers();
        } catch (Throwable e) {
            close();
            Threads.interruptIfNecessary(e);
            throw new Error("Redis init discover error.", e);
        }
    }

    @Override
    public boolean isConnected() {
        Boolean result = stringRedisTemplate.execute((RedisCallback<Boolean>) conn -> !conn.isClosed());
        return result != null && result;
    }

    // ------------------------------------------------------------------Registry

    @Override
    public final void register(R server) {
        if (state.isStopped()) {
            return;
        }

        doRegisterServers(Collections.singleton(server));
        registered.add(server);
        ThrowingRunnable.doCaught(() -> publishRegistryEvent(RegistryEventType.REGISTER, server));
        log.info("Server registered: {}, {}", registryRole, server);
    }

    @Override
    public final void deregister(R server) {
        registered.remove(server);
        ThrowingSupplier.doCaught(() -> stringRedisTemplate.opsForZSet().remove(registryRootPath, server.serialize()));
        ThrowingRunnable.doCaught(() -> publishRegistryEvent(RegistryEventType.DEREGISTER, server));
        log.info("Server deregister: {}, {}", registryRole, server);
    }

    @Override
    public List<R> getRegisteredServers() {
        @SuppressWarnings("unchecked")
        List<String> registryServers = stringRedisTemplate.execute(
            QUERY_SCRIPT, registryRedisKey, Long.toString(System.currentTimeMillis()), REDIS_KEY_TTL_MILLIS
        );
        return deserializeRegistryServers(registryServers);
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
        ThreadPoolExecutors.shutdown(redisSubscribeExecutor, 2);
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
            TextTokenizer tokenizer = new TextTokenizer(message, COLON);
            String type = tokenizer.next();
            String server = tokenizer.tail();

            log.info("Subscribed message: {}, {}", message, channel);
            subscribeDiscoveryEvent(RegistryEventType.valueOf(type), discoveryRole.deserialize(server));
        } catch (Throwable t) {
            log.error("Parse subscribed message error: " + message + ", " + channel, t);
        }
    }

    // ------------------------------------------------------------------private methods

    private void publishRegistryEvent(RegistryEventType eventType, R server) {
        String message = eventType.name() + COLON + server.serialize();
        stringRedisTemplate.convertAndSend(registryChannel, message);
    }

    private void subscribeDiscoveryEvent(RegistryEventType eventType, D server) {
        tryDiscoverServers();
    }

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
    private void doRegisterServers(Set<R> servers) {
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

    private void tryDiscoverServers() {
        if (asyncRefreshLock.tryLock()) {
            try {
                doDiscoverServers();
            } catch (Throwable t) {
                Threads.interruptIfNecessary(t);
                log.error("Redis discover servers occur error.", t);
            } finally {
                asyncRefreshLock.unlock();
            }
        }
    }

    private void doDiscoverServers() throws Throwable {
        RetryTemplate.execute(() -> {
            @SuppressWarnings("unchecked")
            List<String> discovered = stringRedisTemplate.execute(
                QUERY_SCRIPT, discoveryRedisKey, Long.toString(System.currentTimeMillis()), REDIS_KEY_TTL_MILLIS
            );

            if (CollectionUtils.isEmpty(discovered)) {
                log.warn("Not discovered available {} from redis.", discoveryRole);
                discovered = Collections.emptyList();
            }

            List<D> servers = discovered.stream().<D>map(discoveryRole::deserialize).collect(Collectors.toList());
            refreshDiscoveredServers(servers);

            renewNextDiscoverTimeMillis();
            log.debug("Redis discovered {} servers.", discoveryRole);
        }, 3, 1000L);
    }

    private boolean requireDiscoverServers() {
        return state.isRunning() && nextDiscoverTimeMillis < System.currentTimeMillis();
    }

    private void renewNextDiscoverTimeMillis() {
        this.nextDiscoverTimeMillis = System.currentTimeMillis() + periodMs;
    }

}

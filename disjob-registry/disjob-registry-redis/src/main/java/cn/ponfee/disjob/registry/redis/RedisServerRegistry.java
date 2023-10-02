/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.registry.redis;

import cn.ponfee.disjob.common.base.LoopProcessThread;
import cn.ponfee.disjob.common.base.RetryTemplate;
import cn.ponfee.disjob.common.concurrent.NamedThreadFactory;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.concurrent.Threads;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingSupplier;
import cn.ponfee.disjob.common.util.ObjectUtils;
import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.registry.EventType;
import cn.ponfee.disjob.registry.ServerRegistry;
import cn.ponfee.disjob.registry.redis.configuration.RedisRegistryProperties;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
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

    private static final RedisScript<List> DISCOVERY_SCRIPT = RedisScript.of(
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
     * Register period milliseconds
     */
    private final long registryPeriodMs;

    // -------------------------------------------------Registry

    private final LoopProcessThread registerHeartbeatThread;
    private final List<String> registryRedisKey;

    // -------------------------------------------------Discovery

    private final ThreadPoolExecutor asyncRefreshExecutor;
    private final Lock asyncRefreshLock = new ReentrantLock();
    private final List<String> discoveryRedisKey;
    private volatile long nextRefreshTimeMillis = 0;

    // -------------------------------------------------Subscribe

    private final RedisMessageListenerContainer redisMessageListenerContainer;

    protected RedisServerRegistry(StringRedisTemplate stringRedisTemplate,
                                  RedisRegistryProperties config) {
        super(config.getNamespace(), ':');
        this.registryChannel = registryRootPath + separator + CHANNEL;
        this.stringRedisTemplate = stringRedisTemplate;
        this.sessionTimeoutMs = config.getSessionTimeoutMs();
        this.registryPeriodMs = config.getSessionTimeoutMs() / 3;
        this.registryRedisKey = Collections.singletonList(registryRootPath);
        this.discoveryRedisKey = Collections.singletonList(discoveryRootPath);

        ThrowingRunnable<?> action = () -> RetryTemplate.execute(() -> doRegister(registered), 3, 1000);
        this.registerHeartbeatThread = new LoopProcessThread(
            "redis_register_heartbeat", registryPeriodMs, registryPeriodMs, action
        );
        registerHeartbeatThread.start();

        this.asyncRefreshExecutor = ThreadPoolExecutors.builder()
            .corePoolSize(1)
            .maximumPoolSize(1)
            .workQueue(new LinkedBlockingQueue<>())
            .keepAliveTimeSeconds(600)
            .rejectedHandler(ThreadPoolExecutors.CALLER_RUNS)
            .threadFactory(NamedThreadFactory.builder().prefix("redis_async_discovery").priority(Thread.MAX_PRIORITY).build())
            .build();

        // redis pub/sub
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(Objects.requireNonNull(stringRedisTemplate.getConnectionFactory()));
        container.setTaskExecutor(asyncRefreshExecutor);
        // validate “handleMessage” method is valid
        String listenerMethod = ThrowingSupplier.get(() -> RedisServerRegistry.class.getMethod("handleMessage", String.class, String.class).getName());
        MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(this, listenerMethod);
        listenerAdapter.afterPropertiesSet();
        container.addMessageListener(listenerAdapter, new ChannelTopic(discoveryRootPath + separator + CHANNEL));
        container.afterPropertiesSet();
        container.start();
        this.redisMessageListenerContainer = container;

        try {
            doRefreshDiscoveryServers();
        } catch (Throwable e) {
            Threads.interruptIfNecessary(e);
            close();
            throw new Error("Redis registry init discovery error.", e);
        }
    }

    @Override
    public boolean isConnected() {
        Boolean result = stringRedisTemplate.execute((RedisCallback<Boolean>) conn -> !conn.isClosed());
        return result != null && result;
    }

    // ------------------------------------------------------------------redis的pub/sub并不是很可靠，所以这里去定时刷新

    @Override
    public final List<D> getDiscoveredServers(String group) {
        asyncRefreshDiscoveryServers();
        return super.getDiscoveredServers(group);
    }

    @Override
    public final boolean hasDiscoveredServers() {
        asyncRefreshDiscoveryServers();
        return super.hasDiscoveredServers();
    }

    @Override
    public final boolean isDiscoveredServer(D server) {
        asyncRefreshDiscoveryServers();
        return super.isDiscoveredServer(server);
    }

    // ------------------------------------------------------------------Registry

    @Override
    public final void register(R server) {
        if (closed.get()) {
            return;
        }

        doRegister(Collections.singleton(server));
        registered.add(server);
        ThrowingRunnable.execute(() -> publish(server, EventType.REGISTER));
        log.info("Server registered: {} | {}", registryRole.name(), server);
    }

    @Override
    public final void deregister(R server) {
        registered.remove(server);
        ThrowingSupplier.execute(() -> stringRedisTemplate.opsForZSet().remove(registryRootPath, server.serialize()));
        ThrowingRunnable.execute(() -> publish(server, EventType.DEREGISTER));
        log.info("Server deregister: {} | {}", registryRole.name(), server);
    }

    // ------------------------------------------------------------------Close

    @PreDestroy
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            log.warn("Repeat call close method\n{}", ObjectUtils.getStackTrace());
            return;
        }

        registerHeartbeatThread.terminate();
        registered.forEach(this::deregister);
        ThrowingRunnable.execute(redisMessageListenerContainer::stop);
        ThrowingRunnable.execute(() -> ThreadPoolExecutors.shutdown(asyncRefreshExecutor, 2));
        registered.clear();
        super.close();
    }

    // ------------------------------------------------------------------Subscribe

    /**
     * For redis message subscribe invoke.
     *
     * @param message the message
     * @param pattern the pattern
     */
    public void handleMessage(String message, String pattern) {
        try {
            int pos = -1;
            String s0 = message.substring(pos += 1, pos = message.indexOf(COLON, pos));
            String s1 = message.substring(pos += 1);

            log.info("Subscribed message: {} | {}", pattern, message);
            subscribe(EventType.valueOf(s0), discoveryRole.deserialize(s1));
        } catch (Throwable t) {
            log.error("Parse subscribed message error: " + message + ", " + pattern, t);
        }
    }

    // ------------------------------------------------------------------private methods

    private void publish(R server, EventType eventType) {
        String publish = eventType.name() + COLON + server.serialize();
        stringRedisTemplate.convertAndSend(registryChannel, publish);
    }

    private void subscribe(EventType eventType, D server) {
        // refresh the discovery
        resetRefresh();
        asyncRefreshDiscoveryServers();
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
    private void doRegister(Set<R> servers) {
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

    private void asyncRefreshDiscoveryServers() {
        if (!requireRefresh()) {
            return;
        }
        asyncRefreshExecutor.execute(() -> {
            if (!requireRefresh()) {
                return;
            }
            if (!asyncRefreshLock.tryLock()) {
                return;
            }
            try {
                doRefreshDiscoveryServers();
            } catch (Throwable t) {
                Threads.interruptIfNecessary(t);
                log.error("Redis async refresh discovery servers occur error.", t);
            } finally {
                asyncRefreshLock.unlock();
            }
        });
    }

    private void doRefreshDiscoveryServers() throws Throwable {
        RetryTemplate.execute(() -> {
            List<String> discovered = stringRedisTemplate.execute(
                DISCOVERY_SCRIPT, discoveryRedisKey, Long.toString(System.currentTimeMillis()), REDIS_KEY_TTL_MILLIS
            );

            if (CollectionUtils.isEmpty(discovered)) {
                log.warn("Not discovered available {} from redis.", discoveryRole.name());
                discovered = Collections.emptyList();
            }

            List<D> servers = discovered.stream().<D>map(discoveryRole::deserialize).collect(Collectors.toList());
            refreshDiscoveredServers(servers);

            updateRefresh();
            log.debug("Redis refreshed discovery {} servers.", discoveryRole.name());
        }, 3, 1000L);
    }

    private boolean requireRefresh() {
        return !closed.get() && nextRefreshTimeMillis < System.currentTimeMillis();
    }

    private void updateRefresh() {
        this.nextRefreshTimeMillis = System.currentTimeMillis() + registryPeriodMs;
    }

    private void resetRefresh() {
        log.debug("Reset redis refresh time millis.");
        this.nextRefreshTimeMillis = 0;
    }

}

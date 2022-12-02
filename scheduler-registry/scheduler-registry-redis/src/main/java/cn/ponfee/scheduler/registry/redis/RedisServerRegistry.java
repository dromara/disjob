package cn.ponfee.scheduler.registry.redis;

import cn.ponfee.scheduler.common.base.exception.Throwables;
import cn.ponfee.scheduler.common.concurrent.NamedThreadFactory;
import cn.ponfee.scheduler.common.util.ObjectUtils;
import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.Server;
import cn.ponfee.scheduler.registry.EventType;
import cn.ponfee.scheduler.registry.ServerRegistry;
import cn.ponfee.scheduler.registry.ServerRole;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static cn.ponfee.scheduler.common.base.Constants.COLON;

/**
 * Registry server based redis.
 * <p>https://english.stackexchange.com/questions/25931/unregister-vs-deregister
 *
 * @author Ponfee
 */
public abstract class RedisServerRegistry<R extends Server, D extends Server> extends ServerRegistry<R, D> {

    private static final long REDIS_KEY_TTL_MILLIS = 30L * 86400 * 1000;

    /**
     * Default registry server keep alive in 5000 milliseconds
     * <p>server register heartbeat usual is 1000 milliseconds
     */
    protected static final int DEFAULT_REGISTRY_KEEP_ALIVE_MILLISECONDS = 5000;

    /**
     * Default discovery servers refresh interval milliseconds
     */
    protected static final int DEFAULT_DISCOVERY_REFRESH_INTERVAL_MILLISECONDS = 3000;

    private static final String PUBLISH_SUBSCRIBE_CHANNEL = JobConstants.SCHEDULER_KEY_PREFIX + ".channel";

    private final StringRedisTemplate stringRedisTemplate;

    // -------------------------------------------------Registry

    private final long keepAliveInMillis;
    private final ScheduledThreadPoolExecutor registryScheduledExecutor;

    // -------------------------------------------------Discovery

    private final long refreshIntervalMilliseconds;
    private volatile long nextRefreshTimeMillis = 0;

    // -------------------------------------------------Subscribe

    private final RedisMessageListenerContainer redisMessageListenerContainer;

    public RedisServerRegistry(StringRedisTemplate stringRedisTemplate,
                               long keepAliveInMillis,
                               long refreshIntervalMilliseconds) {
        this.stringRedisTemplate = stringRedisTemplate;

        this.keepAliveInMillis = keepAliveInMillis;
        this.registryScheduledExecutor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("redis_server_registry", true));
        this.registryScheduledExecutor.scheduleAtFixedRate(() -> {
            if (closed) {
                return;
            }
            try {
                doRegister(registered);
            } catch (Throwable t) {
                log.error("Do scheduled register occur error: " + registered, t);
            }
        }, 3, 1, TimeUnit.SECONDS);

        this.refreshIntervalMilliseconds = refreshIntervalMilliseconds;

        // redis pub/sub
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(stringRedisTemplate.getConnectionFactory());
        container.setTaskExecutor(registryScheduledExecutor);
        MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(this, "handleMessage");
        listenerAdapter.afterPropertiesSet();
        container.addMessageListener(listenerAdapter, new ChannelTopic(PUBLISH_SUBSCRIBE_CHANNEL));
        container.afterPropertiesSet();
        container.start();
        this.redisMessageListenerContainer = container;
    }

    @Override
    public final List<D> getDiscoveredServers(String group) {
        if (requireRefresh()) {
            synchronized (this) {
                if (requireRefresh()) {
                    doRefreshDiscoveryServers();
                }
            }
        }
        return super.getDiscoveredServers(group);
    }

    // ------------------------------------------------------------------Registry

    @Override
    public final void register(R server) {
        if (closed) {
            return;
        }

        Throwables.caught(() -> doRegister(Collections.singleton(server)));
        Throwables.caught(() -> publish(server, EventType.REGISTER));
        registered.add(server);
        log.info("Server registered: {} - {}", registryRole.name(), server);
    }

    @Override
    public final void deregister(R server) {
        registered.remove(server);
        Throwables.caught(() -> stringRedisTemplate.opsForZSet().remove(registryRole.key(), server.serialize()));
        Throwables.caught(() -> publish(server, EventType.DEREGISTER));
        log.info("Server deregister: {} - {}", registryRole.name(), server);
    }

    // ------------------------------------------------------------------Discovery

    @Override
    public boolean isDiscoveredServerAlive(D server) {
        ZSetOperations<String, String> zsetOps = stringRedisTemplate.opsForZSet();
        Double aliveTimeMillis = zsetOps.score(discoveryRole.key(), server.serialize());
        return aliveTimeMillis != null && aliveTimeMillis > System.currentTimeMillis();
    }

    // ------------------------------------------------------------------Close

    @Override
    public void close() {
        closed = true;
        if (!close.compareAndSet(false, true)) {
            log.warn("Repeat call close method\n{}", ObjectUtils.getStackTrace());
            return;
        }

        registered.forEach(this::deregister);
        registered.clear();

        Throwables.caught(redisMessageListenerContainer::stop);

        try {
            registryScheduledExecutor.shutdownNow();
            registryScheduledExecutor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Await registry scheduled executor termination occur error.", e);
        }

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
            //String s1 = message.substring(pos += 1, pos = message.indexOf(COLON, pos));
            //String s2 = message.substring(pos += 1);

            ServerRole role = ServerRole.valueOf(s0);
            //RegistryEvent event = RegistryEvent.valueOf(s1);
            //D server = role.deserialize(s2);
            subscribe(null, role, null);
            if (role == discoveryRole) {
                log.info("Subscribed message: {} - {}", pattern, message);
            }
        } catch (Throwable t) {
            log.error("Parse subscribed message error: " + message + ", " + pattern, t);
        }
    }

    // ------------------------------------------------------------------private methods

    private void publish(R server, EventType eventType) {
        String publish = registryRole.name() + COLON + eventType.name() + COLON + server.serialize();
        stringRedisTemplate.convertAndSend(PUBLISH_SUBSCRIBE_CHANNEL, publish);
    }

    private void subscribe(D server, ServerRole role, EventType eventType) {
        if (role == discoveryRole) {
            // refresh the discovery
            synchronized (this) {
                doRefreshDiscoveryServers();
            }
        }
    }

    private void doRegister(Set<R> servers) {
        if (CollectionUtils.isEmpty(servers)) {
            return;
        }

        Double score = (double) (System.currentTimeMillis() + keepAliveInMillis);
        Set<ZSetOperations.TypedTuple<String>> tuples = servers
            .stream()
            .map(e -> ZSetOperations.TypedTuple.of(e.serialize(), score))
            .collect(Collectors.toSet());

        stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public Void execute(RedisOperations operations) {
                String registryKey = registryRole.key();
                operations.opsForZSet().add(registryKey, tuples);
                operations.expire(registryKey, REDIS_KEY_TTL_MILLIS, TimeUnit.MILLISECONDS);

                // in pipelined, must return null
                return null;
            }
        });
    }

    private void doRefreshDiscoveryServers() {
        String discoveryKey = discoveryRole.key();
        long now = System.currentTimeMillis();
        List<Object> result = stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public Void execute(RedisOperations operations) {
                operations.opsForZSet().removeRangeByScore(discoveryKey, 0, now);
                operations.opsForZSet().rangeByScore(discoveryKey, now, Long.MAX_VALUE);
                operations.expire(discoveryKey, REDIS_KEY_TTL_MILLIS, TimeUnit.MILLISECONDS);

                // in pipelined, must return null
                return null;
            }
        });

        Set<String> discovered = (Set<String>) result.get(1);
        if (CollectionUtils.isEmpty(discovered)) {
            log.error("Not discovered available {} from redis.", discoveryRole.name());
            discovered = Collections.emptySet();
        }

        List<D> servers = discovered.stream()
                                    .map(s -> (D) discoveryRole.deserialize(s))
                                    .collect(Collectors.toList());
        refreshDiscoveredServers(servers);

        updateRefresh();
        log.debug("Refreshed discovery {}", discoveryRole.name());
    }

    private boolean requireRefresh() {
        return nextRefreshTimeMillis < System.currentTimeMillis();
    }

    private void updateRefresh() {
        this.nextRefreshTimeMillis = System.currentTimeMillis() + refreshIntervalMilliseconds;
    }

}

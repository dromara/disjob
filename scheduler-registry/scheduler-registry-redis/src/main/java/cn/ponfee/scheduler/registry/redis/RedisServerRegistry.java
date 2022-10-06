package cn.ponfee.scheduler.registry.redis;

import cn.ponfee.scheduler.common.base.exception.Throwables;
import cn.ponfee.scheduler.common.concurrent.NamedThreadFactory;
import cn.ponfee.scheduler.common.util.ObjectUtils;
import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.Server;
import cn.ponfee.scheduler.registry.RegistryEvent;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static cn.ponfee.scheduler.common.base.Constants.COLON;

/**
 * Registry server based redis.
 * <p>https://english.stackexchange.com/questions/25931/unregister-vs-deregister
 *
 * @author Ponfee
 */
public abstract class RedisServerRegistry<R extends Server, D extends Server> extends ServerRegistry<R, D> {

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
        this.registryScheduledExecutor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("Redis-Register-Heartbeat-Executor", true));
        this.registryScheduledExecutor.scheduleAtFixedRate(() -> {
            if (closed) {
                return;
            }
            try {
                doRegister(registered);
            } catch (Throwable t) {
                logger.error("Do scheduled register occur error: " + registered, t);
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

    // ------------------------------------------------------------------Registry

    @Override
    public final void register(R server) {
        if (closed) {
            return;
        }

        registered.add(server);
        Throwables.cached(() -> doRegister(Collections.singleton(server)));
        Throwables.cached(() -> publish(server, RegistryEvent.REGISTER));
        logger.info("Server registered: {} - {}", registryRole.name(), server.serialize());
    }

    @Override
    public final void deregister(R server) {
        registered.remove(server);
        Throwables.cached(() -> stringRedisTemplate.opsForZSet().remove(registryRole.registryKey(), server.serialize()));
        Throwables.cached(() -> publish(server, RegistryEvent.DEREGISTER));
        logger.info("Server deregister: {} - {}", registryRole.name(), server.serialize());
    }

    // ------------------------------------------------------------------Discovery

    @Override
    public final List<D> getServers(String group) {
        return getServers(group, false);
    }

    /**
     * Gets servers
     *
     * @param group        the group
     * @param forceRefresh the force refresh
     * @return list of servers
     */
    protected abstract List<D> getServers(String group, boolean forceRefresh);

    @Override
    public final boolean isAlive(D server) {
        ZSetOperations<String, String> zsetOps = stringRedisTemplate.opsForZSet();
        Double aliveTimeMillis = zsetOps.score(discoveryRole.registryKey(), server.serialize());
        return aliveTimeMillis != null && aliveTimeMillis > System.currentTimeMillis();
    }

    // ------------------------------------------------------------------Close

    @Override
    public void close() {
        closed = true;
        if (!close.compareAndSet(false, true)) {
            logger.warn("Repeat call close method\n{}", ObjectUtils.getStackTrace());
            return;
        }

        try {
            registryScheduledExecutor.shutdownNow();
            registryScheduledExecutor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Await registry scheduled executor termination occur error.", e);
        }

        registered.forEach(this::deregister);
        registered.clear();

        Throwables.cached(redisMessageListenerContainer::stop);
    }

    // ------------------------------------------------------------------Subscribe
    @Override
    public void publish(R server, RegistryEvent event) {
        stringRedisTemplate.convertAndSend(PUBLISH_SUBSCRIBE_CHANNEL, buildPublishValue(event, server));
    }

    @Override
    public void subscribe(D server, ServerRole role, RegistryEvent event) {
        if (role == discoveryRole) {
            // refresh the discovery
            getServers(null, true);
        }
    }

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
                logger.info("Subscribed message: {} - {}", pattern, message);
            }
        } catch (Throwable t) {
            logger.error("Parse subscribed message error: " + message + ", " + pattern, t);
        }
    }

    // ------------------------------------------------------------------protected methods

    protected void refreshDiscovery(Consumer<List<D>> processor, boolean forceRefresh) {
        if (forceRefresh) {
            doRefreshDiscovery(processor);
            return;
        }

        if (!requireRefresh()) {
            return;
        }

        synchronized (this) {
            if (!requireRefresh()) {
                return;
            }
            doRefreshDiscovery(processor);
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
                String registryKey = registryRole.registryKey();
                operations.opsForZSet().add(registryKey, tuples);
                operations.expire(registryKey, JobConstants.REDIS_KEY_TTL_SECONDS, TimeUnit.SECONDS);

                // in pipelined, must return null
                return null;
            }
        });
    }

    private synchronized void doRefreshDiscovery(Consumer<List<D>> processor) {
        String discoveryKey = discoveryRole.registryKey();
        long now = System.currentTimeMillis();
        List<Object> result = stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public Void execute(RedisOperations operations) {
                operations.opsForZSet().removeRangeByScore(discoveryKey, 0, now);
                operations.opsForZSet().rangeByScore(discoveryKey, now, Long.MAX_VALUE);
                operations.expire(discoveryKey, JobConstants.REDIS_KEY_TTL_SECONDS, TimeUnit.SECONDS);

                // in pipelined, must return null
                return null;
            }
        });

        Set<String> discovered = (Set<String>) result.get(1);
        if (CollectionUtils.isEmpty(discovered)) {
            logger.error("Discovered from redis failed, Not found available server.");
            discovered = Collections.emptySet();
        }
        processor.accept(discovered.stream().map(s -> (D) discoveryRole.deserialize(s)).collect(Collectors.toList()));

        updateRefresh();
        logger.debug("Refreshed discovery {}", discoveryRole.name());
    }

    private String buildPublishValue(RegistryEvent event, R server) {
        return registryRole.name() + COLON + event.name() + COLON + server.serialize();
    }

    private boolean requireRefresh() {
        return nextRefreshTimeMillis < System.currentTimeMillis();
    }

    private void updateRefresh() {
        this.nextRefreshTimeMillis = System.currentTimeMillis() + refreshIntervalMilliseconds;
    }

}

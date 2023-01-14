/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.registry.redis;

import cn.ponfee.scheduler.common.base.exception.Throwables;
import cn.ponfee.scheduler.common.concurrent.NamedThreadFactory;
import cn.ponfee.scheduler.common.util.ObjectUtils;
import cn.ponfee.scheduler.core.base.Server;
import cn.ponfee.scheduler.registry.EventType;
import cn.ponfee.scheduler.registry.ServerRegistry;
import cn.ponfee.scheduler.registry.redis.configuration.RedisRegistryProperties;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.redis.core.*;
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
 * <p><a href="https://english.stackexchange.com/questions/25931/unregister-vs-deregister">unregister-vs-deregister</a>
 *
 * @author Ponfee
 */
public abstract class RedisServerRegistry<R extends Server, D extends Server> extends ServerRegistry<R, D> {

    private static final long REDIS_KEY_TTL_MILLIS = 30L * 86400 * 1000;
    private static final String CHANNEL = "channel";

    /**
     * Registry publish redis message channel
     */
    private final String registryChannel;

    /**
     * Spring string redis template
     */
    private final StringRedisTemplate stringRedisTemplate;

    // -------------------------------------------------Registry

    private final long sessionTimeoutMs;
    private final ScheduledThreadPoolExecutor registryScheduledExecutor;

    // -------------------------------------------------Discovery

    private final long discoveryPeriodMs;
    private volatile long nextRefreshTimeMillis = 0;

    // -------------------------------------------------Subscribe

    private final RedisMessageListenerContainer redisMessageListenerContainer;

    public RedisServerRegistry(String namespace,
                               StringRedisTemplate stringRedisTemplate,
                               RedisRegistryProperties config) {
        super(namespace, ':');
        this.registryChannel = registryRootPath + separator + CHANNEL;
        String discoveryChannel = discoveryRootPath + separator + CHANNEL;
        this.stringRedisTemplate = stringRedisTemplate;
        this.sessionTimeoutMs = config.getSessionTimeoutMs();
        long registryPeriodMs = config.getRegistryPeriodMs();
        this.registryScheduledExecutor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("redis_server_registry", true));
        this.registryScheduledExecutor.scheduleWithFixedDelay(() -> {
            if (closed.get()) {
                return;
            }
            try {
                doRegister(registered);
            } catch (Throwable t) {
                log.error("Do scheduled register occur error: " + registered, t);
            }
        }, registryPeriodMs, registryPeriodMs, TimeUnit.MILLISECONDS);

        this.discoveryPeriodMs = config.getDiscoveryPeriodMs();

        // redis pub/sub
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(stringRedisTemplate.getConnectionFactory());
        container.setTaskExecutor(registryScheduledExecutor);
        MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(this, "subscribe");
        listenerAdapter.afterPropertiesSet();
        container.addMessageListener(listenerAdapter, new ChannelTopic(discoveryChannel));
        container.afterPropertiesSet();
        container.start();
        this.redisMessageListenerContainer = container;

        doRefreshDiscoveryServers();
    }

    @Override
    public boolean isConnected() {
        Boolean result = stringRedisTemplate.execute((RedisCallback<Boolean>) conn -> !conn.isClosed());
        return result != null && result;
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
        if (closed.get()) {
            return;
        }

        doRegister(Collections.singleton(server));
        registered.add(server);
        publish(server, EventType.REGISTER);
        log.info("Server registered: {} - {}", registryRole.name(), server);
    }

    @Override
    public final void deregister(R server) {
        registered.remove(server);
        Throwables.caught(() -> stringRedisTemplate.opsForZSet().remove(registryRootPath, server.serialize()));
        Throwables.caught(() -> publish(server, EventType.DEREGISTER));
        log.info("Server deregister: {} - {}", registryRole.name(), server);
    }

    // ------------------------------------------------------------------Discovery

    @Override
    public boolean isDiscoveredServerAlive(D server) {
        ZSetOperations<String, String> zsetOps = stringRedisTemplate.opsForZSet();
        Double aliveTimeMillis = zsetOps.score(discoveryRootPath, server.serialize());
        return aliveTimeMillis != null && aliveTimeMillis > System.currentTimeMillis();
    }

    // ------------------------------------------------------------------Close

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            log.warn("Repeat call close method\n{}", ObjectUtils.getStackTrace());
            return;
        }

        Throwables.caught(() -> redisMessageListenerContainer.stop());
        Throwables.caught(registryScheduledExecutor::shutdownNow);
        registered.forEach(this::deregister);
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
    public void subscribe(String message, String pattern) {
        try {
            int pos = -1;
            String s0 = message.substring(pos += 1, pos = message.indexOf(COLON, pos));
            String s1 = message.substring(pos += 1);

            log.info("Subscribed message: {} - {}", pattern, message);
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

    private synchronized void subscribe(EventType eventType, D server) {
        // refresh the discovery
        doRefreshDiscoveryServers();
    }

    private void doRegister(Set<R> servers) {
        if (CollectionUtils.isEmpty(servers)) {
            return;
        }

        Double score = (double) (System.currentTimeMillis() + sessionTimeoutMs);
        Set<ZSetOperations.TypedTuple<String>> tuples = servers
            .stream()
            .map(e -> ZSetOperations.TypedTuple.of(e.serialize(), score))
            .collect(Collectors.toSet());

        stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public Void execute(RedisOperations operations) {
                operations.opsForZSet().add(registryRootPath, tuples);
                operations.expire(registryRootPath, REDIS_KEY_TTL_MILLIS, TimeUnit.MILLISECONDS);

                // in pipelined, must return null
                return null;
            }
        });
    }

    private void doRefreshDiscoveryServers() {
        long now = System.currentTimeMillis();
        List<Object> result = stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public Void execute(RedisOperations operations) {
                operations.opsForZSet().removeRangeByScore(discoveryRootPath, 0, now);
                operations.opsForZSet().rangeByScore(discoveryRootPath, now, Long.MAX_VALUE);
                operations.expire(discoveryRootPath, REDIS_KEY_TTL_MILLIS, TimeUnit.MILLISECONDS);

                // in pipelined, must return null
                return null;
            }
        });

        Set<String> discovered = (Set<String>) result.get(1);
        if (CollectionUtils.isEmpty(discovered)) {
            log.error("Not discovered available {} from redis.", discoveryRole.name());
            discovered = Collections.emptySet();
        }

        List<D> servers = discovered.stream().map(s -> (D) discoveryRole.deserialize(s)).collect(Collectors.toList());
        refreshDiscoveredServers(servers);

        updateRefresh();
    }

    private boolean requireRefresh() {
        return nextRefreshTimeMillis < System.currentTimeMillis();
    }

    private void updateRefresh() {
        this.nextRefreshTimeMillis = System.currentTimeMillis() + discoveryPeriodMs;
    }

}

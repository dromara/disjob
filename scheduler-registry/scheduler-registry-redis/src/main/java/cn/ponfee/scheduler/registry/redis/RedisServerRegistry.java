package cn.ponfee.scheduler.registry.redis;

import cn.ponfee.scheduler.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.scheduler.common.util.ObjectUtils;
import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.Server;
import cn.ponfee.scheduler.registry.Actions;
import cn.ponfee.scheduler.registry.Roles;
import cn.ponfee.scheduler.registry.ServerRegistry;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.beans.Transient;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static cn.ponfee.scheduler.common.base.Constants.SEMICOLON;

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

    protected static final String PUBLISH_SUBSCRIBE_CHANNEL = JobConstants.SCHEDULER_KEY_PREFIX + ".channel";

    private final StringRedisTemplate stringRedisTemplate;

    // -------------------------------------------------Registry

    private final long keepAliveInMillis;
    private final ScheduledThreadPoolExecutor registryScheduledExecutor;

    // -------------------------------------------------Discovery

    private final long refreshIntervalMilliseconds;
    private final RestTemplate restTemplate;
    private volatile long nextRefreshTimeMillis = 0;

    // ------------------------------------------------------------------Subscribe

    private final RedisMessageListenerContainer redisMessageListenerContainer;

    public RedisServerRegistry(StringRedisTemplate stringRedisTemplate,
                               long keepAliveInMillis,
                               long refreshIntervalMilliseconds) {
        this.stringRedisTemplate = stringRedisTemplate;

        this.keepAliveInMillis = keepAliveInMillis;
        this.registryScheduledExecutor = new ScheduledThreadPoolExecutor(1, ThreadPoolExecutors.DISCARD);
        this.registryScheduledExecutor.scheduleAtFixedRate(() -> {
            if (closed) {
                return;
            }
            try {
                doRegister(registered);
            } catch (Throwable t) {
                logger.error("Do scheduled register occur error: " + registered, t);
            }
        }, 15, 1, TimeUnit.SECONDS);

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

        // http spring reset template
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(200);
        requestFactory.setReadTimeout(300);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    // ------------------------------------------------------------------Registry

    @Override
    public final void register(R server) {
        if (closed) {
            return;
        }

        registered.add(server);
        doRegister(Collections.singleton(server));
        stringRedisTemplate.convertAndSend(PUBLISH_SUBSCRIBE_CHANNEL, buildPublishValue(Actions.REGISTER, server));
        logger.info("Server registered: {} - {}", registryRole.name(), server.serialize());
    }

    @Override
    public final void deregister(R server) {
        registered.remove(server);
        stringRedisTemplate.opsForZSet().remove(registryRole.registryKey(), server.serialize());
        stringRedisTemplate.convertAndSend(PUBLISH_SUBSCRIBE_CHANNEL, buildPublishValue(Actions.DEREGISTER, server));
        logger.info("Server deregister: {} - {}", registryRole.name(), server.serialize());
    }

    // ------------------------------------------------------------------Discovery

    @Override
    public final List<D> getServers(String group) {
        return getServers(group, false);
    }

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
        } catch (InterruptedException e) {
            logger.error("Await registry scheduled executor termination occur error.", e);
            Thread.currentThread().interrupt();
        }

        registered.forEach(this::deregister);
        registered.clear();

        redisMessageListenerContainer.stop();
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
            String s0 = message.substring(pos += 1, pos = message.indexOf(SEMICOLON, pos));
            //String s1 = message.substring(pos += 1, pos = message.indexOf(SEMICOLON, pos));
            //String s2 = message.substring(pos += 1);

            Roles role = Roles.valueOf(s0);
            //Actions action = Actions.valueOf(s1);
            //D server = role.deserialize(s2);
            if (role == discoveryRole) {
                getServers(null, true);
                logger.info("Subscribed message: {} - {}", pattern, message);
            }
        } catch (Exception e) {
            logger.error("Parse subscribed message error: " + message + ", " + pattern, e);
        }
    }

    // ------------------------------------------------------------------protected methods

    protected void doRefreshDiscoveryInSynchronized(Consumer<Set<String>> processor, boolean forceRefresh) {
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

    protected List<D> filterAvailableServers(List<D> discoveredServers) {
        if (discoveredServers.isEmpty()) {
            return Collections.emptyList();
        }

        return discoveredServers
            .parallelStream()
            .filter(supervisor -> {
                String url = String.format("http://%s:%d/%s", supervisor.getHost(), supervisor.getPort(), "actuator/health");
                try {
                    ActuatorHealth result = restTemplate.getForObject(url, ActuatorHealth.class);
                    return result != null && result.isSuccess();
                } catch (Exception e) {
                    logger.warn("Not available supervisor: {}, {}", supervisor, e.getMessage());
                    return false;
                }
            })
            .collect(Collectors.toList());
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

    private synchronized void doRefreshDiscovery(Consumer<Set<String>> processor) {
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
        processor.accept(discovered);

        updateRefresh();
        logger.info("Refreshed discovery " + discoveryRole.name());
    }

    private String buildPublishValue(Actions action, R server) {
        return registryRole.name() + SEMICOLON + action.name() + SEMICOLON + server.serialize();
    }

    private boolean requireRefresh() {
        return nextRefreshTimeMillis < System.currentTimeMillis();
    }

    private void updateRefresh() {
        this.nextRefreshTimeMillis = System.currentTimeMillis() + refreshIntervalMilliseconds;
    }

    @Data
    private static class ActuatorHealth {
        private String status;

        @Transient
        public boolean isSuccess() {
            return "UP".equals(status);
        }
    }

}

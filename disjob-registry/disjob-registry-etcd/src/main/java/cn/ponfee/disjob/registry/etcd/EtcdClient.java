/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.registry.etcd;

import cn.ponfee.disjob.common.concurrent.LoopThread;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.util.ClassUtils;
import cn.ponfee.disjob.common.util.Fields;
import cn.ponfee.disjob.registry.ConnectionStateListener;
import cn.ponfee.disjob.registry.etcd.configuration.EtcdRegistryProperties;
import com.google.common.collect.ImmutableList;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.lease.LeaseTimeToLiveResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.LeaseOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.support.CloseableClient;
import io.etcd.jetcd.support.Observers;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchResponse;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Etcd client
 *
 * @author Ponfee
 */
public class EtcdClient implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(EtcdClient.class);

    private static final List<ConnectivityState> CONNECTED_STATUS_LIST = ImmutableList.of(
        ConnectivityState.READY, ConnectivityState.IDLE
    );

    private static final List<WatchEvent.EventType> CHANGED_EVENT_TYPES = ImmutableList.of(
        WatchEvent.EventType.PUT, WatchEvent.EventType.DELETE
    );

    private static final GetOption GET_PREFIX_OPTION = GetOption.builder().isPrefix(true).build();

    private static final GetOption GET_COUNT_OPTION = GetOption.builder().withCountOnly(true).build();

    private static final WatchOption WATCH_PREFIX_OPTION = WatchOption.builder().isPrefix(true).build();

    /**
     * Etcd properties
     */
    private final EtcdRegistryProperties config;

    /**
     * Etcd native client
     */
    private final Client client;

    /**
     * Health check thread
     */
    private final LoopThread healthCheckThread;

    private final Map<String, Pair<Watch.Watcher, ChildChangedListener>> childWatchers = new HashMap<>();

    private final Set<ConnectionStateListener<EtcdClient>> connectionStateListeners = ConcurrentHashMap.newKeySet();

    private volatile boolean lastConnectState;

    public EtcdClient(EtcdRegistryProperties config) {
        this.config = config;
        this.client = Client.builder()
            .endpoints(config.endpoints())
            .maxInboundMessageSize(config.getMaxInboundMessageSize())
            .build();
        this.lastConnectState = isConnected();

        long periodMs = 3000;
        this.healthCheckThread = LoopThread.createStarted("etcd_health_check", periodMs, periodMs, () -> {
            boolean currConnectState = isConnected();
            if (lastConnectState == currConnectState) {
                return;
            }
            for (ConnectionStateListener<EtcdClient> listener : connectionStateListeners) {
                try {
                    if (currConnectState) {
                        listener.onConnected(this);
                    } else {
                        listener.onDisconnected(this);
                    }
                } catch (Throwable t) {
                    LOG.error("Notify connection state changed occur error: " + currConnectState, t);
                }
            }
            this.lastConnectState = currConnectState;
        });
    }

    public void addConnectionStateListener(ConnectionStateListener<EtcdClient> listener) {
        connectionStateListeners.add(listener);
    }

    public void removeConnectionStateListener(ConnectionStateListener<EtcdClient> listener) {
        connectionStateListeners.remove(listener);
    }

    // ----------------------------------------------------------------lease id operations

    public long createLease(long ttl) throws Exception {
        return client.getLeaseClient()
            .grant(ttl)
            .get(config.getRequestTimeoutMs(), TimeUnit.MILLISECONDS)
            .getID();
    }

    public long getLeaseTTL(long leaseId) throws Exception {
        LeaseTimeToLiveResponse resp = client.getLeaseClient()
            .timeToLive(leaseId, LeaseOption.DEFAULT)
            .get(config.getRequestTimeoutMs(), TimeUnit.MILLISECONDS);
        return resp == null ? 0 : resp.getTTL();
    }

    public boolean keepAliveOnceLease(long leaseId) throws Exception {
        LeaseKeepAliveResponse resp = client.getLeaseClient()
            .keepAliveOnce(leaseId)
            .get(config.getRequestTimeoutMs(), TimeUnit.MILLISECONDS);
        return resp != null && resp.getTTL() > 0;
    }

    public CloseableClient keepAliveLease(long leaseId, Consumer<Throwable> error, Runnable completed) {
        return keepAliveLease(leaseId, null, error, completed);
    }

    public CloseableClient keepAliveLease(long leaseId, Consumer<LeaseKeepAliveResponse> next,
                                          Consumer<Throwable> error, Runnable completed) {
        return client.getLeaseClient().keepAlive(
            leaseId,
            Observers.<LeaseKeepAliveResponse>builder().onNext(next).onError(error).onCompleted(completed).build()
        );
    }

    public void revokeLease(long leaseId) throws Exception {
        client.getLeaseClient()
            .revoke(leaseId)
            .get(config.getRequestTimeoutMs(), TimeUnit.MILLISECONDS);
    }

    // ----------------------------------------------------------------key operations

    public void createPersistentKey(String key, String value) throws Exception {
        client.getKVClient()
            .put(utf8(key), utf8(value))
            .get(config.getRequestTimeoutMs(), TimeUnit.MILLISECONDS);
    }

    public void createEphemeralKey(String key, String value, long leaseId) throws Exception {
        client.getKVClient()
            .put(utf8(key), utf8(value), PutOption.builder().withLeaseId(leaseId).build())
            .get(config.getRequestTimeoutMs(), TimeUnit.MILLISECONDS);
    }

    public void deleteKey(String key) throws Exception {
        client.getKVClient()
            .delete(utf8(key))
            .get(config.getRequestTimeoutMs(), TimeUnit.MILLISECONDS);
    }

    public boolean existsKey(String key) throws Exception {
        long count = client.getKVClient()
            .get(utf8(key), GET_COUNT_OPTION)
            .get(config.getRequestTimeoutMs(), TimeUnit.MILLISECONDS)
            .getCount();
        return count > 0;
    }

    public List<String> getKeyChildren(String parentKey) throws Exception {
        return client.getKVClient()
            .get(utf8(parentKey), GET_PREFIX_OPTION)
            .get(config.getRequestTimeoutMs(), TimeUnit.MILLISECONDS)
            .getKvs()
            .stream()
            .map(kv -> {
                String key = kv.getKey().toString(UTF_8);
                int childIndex = parentKey.length() + 1;
                return key.length() > childIndex ? key.substring(childIndex) : null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------watch

    public synchronized void watchChildChanged(String parentKey, CountDownLatch latch, Consumer<List<String>> listener) {
        if (childWatchers.containsKey(parentKey)) {
            throw new IllegalStateException("Parent key already watched: " + parentKey);
        }

        ChildChangedListener innerListener = new ChildChangedListener(parentKey, latch, listener);
        Watch.Watcher watcher = client.getWatchClient().watch(utf8(parentKey), WATCH_PREFIX_OPTION, innerListener);
        childWatchers.put(parentKey, Pair.of(watcher, innerListener));
    }

    public synchronized boolean unwatchChildChanged(String parentKey) {
        Pair<Watch.Watcher, ChildChangedListener> pair = childWatchers.remove(parentKey);
        if (pair != null) {
            pair.getLeft().close();
            pair.getRight().close();
            return true;
        } else {
            return false;
        }
    }

    // ----------------------------------------------------------------others

    public boolean isConnected() {
        try {
            Object connectionManager = Fields.get(client, "connectionManager");
            ManagedChannel managedChannel = ClassUtils.invoke(connectionManager, "getChannel");
            ConnectivityState state = managedChannel.getState(false);
            return CONNECTED_STATUS_LIST.contains(state);
        } catch (Throwable t) {
            LOG.error("Detect etcd is connected error", t);
            return false;
        }
    }

    @Override
    public synchronized void close() {
        new ArrayList<>(childWatchers.keySet()).forEach(this::unwatchChildChanged);
        healthCheckThread.terminate();
        client.close();
    }

    // ----------------------------------------------------------------private static classes & methods

    private static ByteSequence utf8(String key) {
        return ByteSequence.from(key, UTF_8);
    }

    private class ChildChangedListener implements Consumer<WatchResponse>, Closeable {
        private final String parentKey;
        private final CountDownLatch latch;
        private final Consumer<List<String>> processor;

        private final ThreadPoolExecutor asyncExecutor = ThreadPoolExecutors.builder()
            .corePoolSize(1)
            .maximumPoolSize(1)
            .workQueue(new LinkedBlockingQueue<>(2))
            .keepAliveTimeSeconds(600)
            .rejectedHandler(ThreadPoolExecutors.DISCARD)
            .build();

        public ChildChangedListener(String parentKey, CountDownLatch latch, Consumer<List<String>> processor) {
            this.parentKey = parentKey;
            this.latch = latch;
            this.processor = processor;
        }

        @PreDestroy
        @Override
        public void close() {
            ThreadPoolExecutors.shutdown(asyncExecutor, 1);
        }

        @Override
        public void accept(WatchResponse response) {
            ThrowingRunnable.execute(latch::await);

            List<WatchEvent> events = response.getEvents();
            if (events.stream().noneMatch(e -> CHANGED_EVENT_TYPES.contains(e.getEventType()))) {
                return;
            }
            if (events.stream().allMatch(e -> parentKey.equals(e.getKeyValue().getKey().toString(UTF_8)))) {
                return;
            }

            asyncExecutor.submit(() -> {
                try {
                    List<String> children = getKeyChildren(parentKey);
                    processor.accept(children);
                } catch (Throwable t) {
                    LOG.error("Get key '" + parentKey + "' children occur error.", t);
                }
            });
        }
    }

}

/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.registry.zookeeper;

import cn.ponfee.scheduler.common.base.exception.Throwables;
import cn.ponfee.scheduler.registry.zookeeper.configuration.ZookeeperRegistryProperties;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Curator framework operations client
 *
 * @author Ponfee
 */
public class CuratorFrameworkClient implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(CuratorFrameworkClient.class);

    private final Map<String, ChildChangedWatcher> childWatchers = new HashMap<>();
    private final CuratorFramework curatorFramework;
    private final ReconnectCallback reconnectCallback;

    public CuratorFrameworkClient(ZookeeperRegistryProperties config, ReconnectCallback reconnectCallback) throws Exception {
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
            .connectString(config.getConnectString())
            .connectionTimeoutMs(config.getConnectionTimeoutMs())
            .sessionTimeoutMs(config.getSessionTimeoutMs())
            .retryPolicy(buildRetryPolicy(config));

        Optional.ofNullable(config.authorization()).ifPresent(s -> builder.authorization("digest", s.getBytes()));

        this.curatorFramework = builder.build();
        curatorFramework.getConnectionStateListenable().addListener(new CuratorConnectionStateListener());

        curatorFramework.start();
        boolean isStarted = curatorFramework.getState().equals(CuratorFrameworkState.STARTED);
        Assert.state(isStarted, () -> "Curator framework not started: " + curatorFramework.getState());
        boolean isConnected = curatorFramework.blockUntilConnected(config.getMaxWaitTimeMs(), TimeUnit.MILLISECONDS);
        Assert.state(isConnected, () -> "Curator framework not connected: " + curatorFramework.getState());

        this.reconnectCallback = reconnectCallback;
    }

    public void createPersistent(String path) throws Exception {
        try {
            curatorFramework.create().creatingParentsIfNeeded().forPath(path);
        } catch (KeeperException.NodeExistsException e) {
            LOG.debug("Node path already exists: {} | {}", path, e.getMessage());
        }
    }

    public void createEphemeral(String path, int retries) throws Exception {
        try {
            curatorFramework.create().withMode(CreateMode.EPHEMERAL).forPath(path);
        } catch (KeeperException.NodeExistsException e) {
            LOG.debug("Node path already exists: {} | {}", path, e.getMessage());
            if (retries > 0) {
                deletePath(path);
                createEphemeral(path, --retries);
            }
        }
    }

    public void createPersistent(String path, byte[] data) throws Exception {
        try {
            curatorFramework.create().creatingParentsIfNeeded().forPath(path, data);
        } catch (KeeperException.NodeExistsException ignored) {
            curatorFramework.setData().forPath(path, data);
        }
    }

    public void createEphemeral(String path, byte[] data, int retries) throws Exception {
        try {
            curatorFramework.create().withMode(CreateMode.EPHEMERAL).forPath(path, data);
        } catch (KeeperException.NodeExistsException ignored) {
            if (retries > 0) {
                deletePath(path);
                createEphemeral(path, data, --retries);
            }
        }
    }

    public void deletePath(String path) throws Exception {
        try {
            curatorFramework.delete()/*.guaranteed()*/.deletingChildrenIfNeeded().forPath(path);
        } catch (KeeperException.NoNodeException e) {
            LOG.debug("Node path not exists: {} | {}", path, e.getMessage());
        }
    }

    public List<String> getChildren(String path) throws Exception {
        try {
            return curatorFramework.getChildren().forPath(path);
        } catch (KeeperException.NoNodeException ignored) {
            return null;
        }
    }

    public byte[] getData(String path) throws Exception {
        try {
            return curatorFramework.getData().forPath(path);
        } catch (KeeperException.NoNodeException ignored) {
            return null;
        }
    }

    public boolean checkExists(String path) {
        try {
            return curatorFramework.checkExists().forPath(path) != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    public synchronized void watchChildChanged(String path, CountDownLatch latch, Consumer<List<String>> processor) throws Exception {
        if (childWatchers.containsKey(path)) {
            throw new IllegalStateException("Path already watched: " + path);
        }

        ChildChangedWatcher watcher = new ChildChangedWatcher(path, latch, processor);
        List<String> servers = curatorFramework.getChildren().usingWatcher(watcher).forPath(path);
        childWatchers.put(path, watcher);
        processor.accept(servers);
    }

    public synchronized boolean unwatchChildChanged(String path) {
        ChildChangedWatcher watcher = childWatchers.remove(path);
        if (watcher != null) {
            watcher.unwatch();
            return true;
        } else {
            return false;
        }
    }

    /*
    public void listenChildChanged(String path) {
        CuratorCacheListener pathChildrenCacheListener = CuratorCacheListener
            .builder()
            .forPathChildrenCache(path, curatorFramework, (client, event) -> {
                switch (event.getType()) {
                    case INITIALIZED:
                        List<String> initServers = Optional.ofNullable(event.getInitialData())
                            .map(e -> e.stream().map(ChildData::getPath).collect(Collectors.toList()))
                            .orElse(Collections.emptyList());
                        LOG.info("curator patch children cache init servers: {}", initServers);
                        // init servers
                        break;
                    case CHILD_ADDED:
                        String additionServer = event.getData().getPath();
                        // add server
                        LOG.info("curator patch children cache add servers: {}", additionServer);
                        break;
                    case CHILD_REMOVED:
                        String removingServer = event.getData().getPath();
                        // remove server
                        LOG.info("curator patch children cache remove servers: {}", removingServer);
                        break;
                    default:
                        LOG.debug("Discard zookeeper event: {} | {}", event.getType(), event.getData().getPath());
                        break;
                }
            })
            .build();
        CuratorCache curatorCache = CuratorCache.build(curatorFramework, path);
        curatorCache.listenable().addListener(pathChildrenCacheListener);
        curatorCache.start();
    }
    */

    public boolean isConnected() {
        return curatorFramework.getZookeeperClient().isConnected();
    }

    @Override
    public synchronized void close() {
        new ArrayList<>(childWatchers.keySet()).forEach(this::unwatchChildChanged);
        curatorFramework.close();
    }

    public interface ReconnectCallback {
        /**
         * callback
         *
         * @param client the client
         */
        void call(CuratorFrameworkClient client);
    }

    // -------------------------------------------------------------------------------private

    /**
     * CuratorWatcher effectively must do watch after CuratorCache start
     */
    private class ChildChangedWatcher implements CuratorWatcher {
        private final String path;
        private final CountDownLatch latch;
        private volatile Consumer<List<String>> processor;

        public ChildChangedWatcher(String path, CountDownLatch latch, Consumer<List<String>> processor) {
            this.path = path;
            this.latch = latch;
            this.processor = processor;
        }

        public void unwatch() {
            this.processor = null;
        }

        @Override
        public void process(WatchedEvent event) throws Exception {
            Throwables.caught((Throwables.ThrowingRunnable<?>) latch::await);
            LOG.info("Watched event type: {}", event.getType());

            final Consumer<List<String>> action = processor;
            if (action == null || event.getType() == Watcher.Event.EventType.None) {
                return;
            }
            List<String> children = curatorFramework.getChildren().usingWatcher(this).forPath(path);
            action.accept(children);
        }
    }

    private class CuratorConnectionStateListener implements ConnectionStateListener {
        private static final long UNKNOWN_SESSION_ID = -1L;
        private long lastSessionId;

        @Override
        public void stateChanged(CuratorFramework client, ConnectionState state) {
            long sessionId;
            try {
                sessionId = client.getZookeeperClient().getZooKeeper().getSessionId();
            } catch (Exception e) {
                sessionId = UNKNOWN_SESSION_ID;
                LOG.warn("Curator client state changed, get session instance error.", e);
            }
            if (state == ConnectionState.CONNECTED) {
                lastSessionId = sessionId;
                LOG.info("Curator first connected, session={}", hex(sessionId));
            } else if (state == ConnectionState.LOST) {
                LOG.warn("Curator session expired, session={}", hex(lastSessionId));
            } else if (state == ConnectionState.SUSPENDED) {
                LOG.warn("Curator connection lost, session={}", hex(sessionId));
            } else if (state == ConnectionState.RECONNECTED) {
                if (lastSessionId == sessionId && sessionId != UNKNOWN_SESSION_ID) {
                    LOG.warn("Curator recover connected, reuse old-session={}", hex(sessionId));
                } else {
                    LOG.warn("Curator recover connected, old-session={}, new-session={}", hex(lastSessionId), hex(sessionId));
                    lastSessionId = sessionId;
                }
                reconnectCallback.call(CuratorFrameworkClient.this);
            }
        }
    }

    private static RetryPolicy buildRetryPolicy(ZookeeperRegistryProperties config) {
        return new ExponentialBackoffRetry(
            config.getBaseSleepTimeMs(),
            config.getMaxRetries(),
            config.getMaxSleepMs()
        );
    }

    private static String hex(long number) {
        return Long.toHexString(number);
    }

}

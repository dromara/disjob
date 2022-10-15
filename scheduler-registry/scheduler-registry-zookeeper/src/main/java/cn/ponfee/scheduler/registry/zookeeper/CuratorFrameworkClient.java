package cn.ponfee.scheduler.registry.zookeeper;

import cn.ponfee.scheduler.registry.zookeeper.configuration.ZookeeperProperties;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Curator framework Utilities
 *
 * @author Ponfee
 */
public class CuratorFrameworkClient implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(CuratorFrameworkClient.class);

    private final Map<String, ChildChangedWatcher> childWatchers = new HashMap<>();
    private final CuratorFramework curatorFramework;
    private final ReconnectCallback reconnectCallback;

    public CuratorFrameworkClient(ZookeeperProperties props, ReconnectCallback reconnectCallback) throws Exception {
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
            .connectString(props.getClusterAddress())
            .connectionTimeoutMs(props.getConnectionTimeoutMs())
            .sessionTimeoutMs(props.getSessionTimeoutMs())
            .retryPolicy(buildRetryPolicy(props));

        Optional.ofNullable(props.authorization()).ifPresent(s -> builder.authorization("digest", s.getBytes()));

        this.curatorFramework = builder.build();
        curatorFramework.getConnectionStateListenable().addListener(new CuratorConnectionStateListener());

        curatorFramework.start();
        boolean isStarted = curatorFramework.getState().equals(CuratorFrameworkState.STARTED);
        Assert.state(isStarted, "Curator framework not started: " + curatorFramework.getState());
        boolean isConnected = curatorFramework.blockUntilConnected(props.getMaxWaitTimeMs(), TimeUnit.MILLISECONDS);
        Assert.state(isConnected, "Curator framework not connected: " + curatorFramework.getState());

        this.reconnectCallback = reconnectCallback;
    }

    public void createPersistent(String path) throws Exception {
        try {
            curatorFramework.create().creatingParentsIfNeeded().forPath(path);
        } catch (KeeperException.NodeExistsException e) {
            LOG.debug("Node path already exists: {} - {}", path, e.getMessage());
        }
    }

    public void createEphemeral(String path) throws Exception {
        try {
            curatorFramework.create().withMode(CreateMode.EPHEMERAL).forPath(path);
        } catch (KeeperException.NodeExistsException e) {
            LOG.debug("Node path already exists: {} - {}", path, e.getMessage());
            deletePath(path);
            createEphemeral(path);
        }
    }

    public void createPersistent(String path, byte[] data) throws Exception {
        try {
            curatorFramework.create().creatingParentsIfNeeded().forPath(path, data);
        } catch (KeeperException.NodeExistsException ignored) {
            curatorFramework.setData().forPath(path, data);
        }
    }

    public void createEphemeral(String path, byte[] data) throws Exception {
        try {
            curatorFramework.create().withMode(CreateMode.EPHEMERAL).forPath(path, data);
        } catch (KeeperException.NodeExistsException ignored) {
            deletePath(path);
            createEphemeral(path, data);
        }
    }

    public void deletePath(String path) throws Exception {
        try {
            curatorFramework.delete()/*.guaranteed()*/.deletingChildrenIfNeeded().forPath(path);
        } catch (KeeperException.NoNodeException e) {
            LOG.debug("Node path not exists: {} - {}", path, e.getMessage());
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

    public synchronized void addChildChangedWatcher(String path, Consumer<List<String>> listener) throws Exception {
        if (childWatchers.containsKey(path)) {
            throw new IllegalStateException("Path already watched: " + path);
        }

        ChildChangedWatcher watcher = new ChildChangedWatcher(path, listener);
        List<String> servers = curatorFramework.getChildren().usingWatcher(watcher).forPath(watcher.path);
        childWatchers.put(path, watcher);
        listener.accept(servers);
    }

    public synchronized boolean removeChildChangedWatcher(String path) {
        ChildChangedWatcher watcher = childWatchers.remove(path);
        if (watcher != null) {
            watcher.unwatch();
            return true;
        } else {
            return false;
        }
    }

    /*
    public void listenChild(String path) {
        CuratorCache curatorCache = CuratorCache.build(curatorFramework, path);
        curatorCache.listenable().addListener((type, oldData, newData) -> {
            LOG.info("Listen new data path: {}", (newData == null ? null : newData.getPath()));
            int parentLength = path.length();
            String newDataPath;
            if (newData != null && (newDataPath = newData.getPath()).length() > parentLength) {
                LOG.info("Listen server: {}", newDataPath.substring(parentLength + 1));
            }
        });
        curatorCache.start();
    }
    */

    public boolean isConnected() {
        return curatorFramework.getZookeeperClient().isConnected();
    }

    @Override
    public synchronized void close() {
        childWatchers.values().forEach(ChildChangedWatcher::unwatch);
        curatorFramework.close();
    }

    public interface ReconnectCallback {
        void call(CuratorFrameworkClient client);
    }

    // -------------------------------------------------------------------------------private

    /**
     * CuratorWatcher effectively must do watch after CuratorCache start
     */
    private class ChildChangedWatcher implements CuratorWatcher {
        private final String path;
        private volatile Consumer<List<String>> listener;

        public ChildChangedWatcher(String path, Consumer<List<String>> listener) {
            this.path = path;
            this.listener = listener;
        }

        public void unwatch() {
            this.listener = null;
        }

        @Override
        public void process(WatchedEvent event) throws Exception {
            LOG.info("Watched event type: " + event.getType());
            if (listener == null || event.getType() == Watcher.Event.EventType.None) {
                return;
            }
            listener.accept(curatorFramework.getChildren().usingWatcher(this).forPath(path));
        }
    }

    private class CuratorConnectionStateListener implements ConnectionStateListener {
        private final long UNKNOWN_SESSION_ID = -1L;
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

        private String hex(long number) {
            return Long.toHexString(number);
        }
    }

    private static RetryPolicy buildRetryPolicy(ZookeeperProperties props) {
        return new ExponentialBackoffRetry(
            props.getBaseSleepTimeMs(),
            props.getMaxRetries(),
            props.getMaxSleepMs()
        );
    }
}

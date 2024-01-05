/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.id.snowflake.zk;

import cn.ponfee.disjob.common.base.IdGenerator;
import cn.ponfee.disjob.common.base.RetryTemplate;
import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.concurrent.Threads;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingFunction;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.util.Bytes;
import cn.ponfee.disjob.common.util.Predicates;
import cn.ponfee.disjob.id.snowflake.ClockMovedBackwardsException;
import cn.ponfee.disjob.id.snowflake.Snowflake;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Snowflake server based zookeeper
 *
 * <pre>
 * /snowflake/{bizTag}
 * ├── tag (children are EPHEMERAL node)
 * │   ├── serverTag-a   data=workerId-1
 * │   ├── serverTag-b   data=workerId-2
 * │   └── serverTag-c   data=workerId-3
 * └── id (children are EPHEMERAL node)
 *     ├── workerId-1    data=lastHeartbeatTime
 *     ├── workerId-2    data=lastHeartbeatTime
 *     └── workerId-3    data=lastHeartbeatTime
 *
 * </pre>
 *
 * @author Ponfee
 */
public class ZkDistributedSnowflake extends SingletonClassConstraint implements IdGenerator, Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(ZkDistributedSnowflake.class);

    private static final long HEARTBEAT_PERIOD_MS = 30000;

    private static final String SEP = "/";

    /**
     * Server tag
     */
    private final String serverTag;

    /**
     * /snowflake/{bizTag}/tag
     */
    private final String serverTagParentPath;

    /**
     * /snowflake/{bizTag}/tag/{serverTag}
     */
    private final String serverTagPath;

    /**
     * /snowflake/{bizTag}/id
     */
    private final String workerIdParentPath;

    /**
     * /snowflake/{bizTag}/id/{workerId}
     */
    private final String workerIdPath;

    private final CuratorFramework curator;
    private final int workerId;
    private final Snowflake snowflake;

    private volatile boolean closed = false;

    public ZkDistributedSnowflake(ZkConfig zkConfig, String bizTag, String serverTag) {
        this(zkConfig, bizTag, serverTag, 14, 8);
    }

    public ZkDistributedSnowflake(ZkConfig zkConfig,
                                  String bizTag,
                                  String serverTag,
                                  int sequenceBitLength,
                                  int workerIdBitLength) {
        Assert.isTrue(!bizTag.contains(SEP), () -> "Biz tag cannot contains '/': " + bizTag);
        Assert.isTrue(!serverTag.contains(SEP), () -> "Server tag cannot contains '/': " + serverTag);
        int len = sequenceBitLength + workerIdBitLength;
        Assert.isTrue(len <= 22, () -> "Bit length(sequence + worker) cannot greater than 22, but actual=" + len);
        this.serverTag = serverTag;
        String snowflakeRootPath = "/snowflake/" + bizTag;
        this.serverTagParentPath = snowflakeRootPath + "/tag";
        this.workerIdParentPath = snowflakeRootPath + "/id";
        this.serverTagPath = serverTagParentPath + SEP + serverTag;

        try {
            this.curator = createCuratorFramework(zkConfig);
            RetryTemplate.execute(() -> createPersistent(snowflakeRootPath), 3, 1000L);
            RetryTemplate.execute(() -> createPersistent(serverTagParentPath), 3, 1000L);
            RetryTemplate.execute(() -> createPersistent(workerIdParentPath), 3, 1000L);
        } catch (Throwable e) {
            Threads.interruptIfNecessary(e);
            throw new Error("Zk snowflake server initialize error.", e);
        }

        try {
            this.workerId = RetryTemplate.execute(() -> registerWorkerId(workerIdBitLength), 5, 2000L);
            this.workerIdPath = workerIdParentPath + SEP + workerId;
            this.snowflake = new Snowflake(workerId, sequenceBitLength, workerIdBitLength);
        } catch (Throwable e) {
            Threads.interruptIfNecessary(e);
            throw new Error("Zk snowflake server registry worker error.", e);
        }

        curator.getConnectionStateListenable().addListener(new CuratorConnectionStateListener(this));

        ThreadPoolExecutors.commonScheduledPool().scheduleWithFixedDelay(this::heartbeat, 0, HEARTBEAT_PERIOD_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public long generateId() {
        return snowflake.generateId();
    }

    @PreDestroy
    @Override
    public void close() {
        closed = true;
    }

    // ------------------------------------------------------------------private methods

    private void createPersistent(String path) throws Exception {
        try {
            curator.create().creatingParentsIfNeeded().forPath(path);
            LOG.info("Created zk persistent path: {}", path);
        } catch (KeeperException.NodeExistsException ignored) {
            // ignored
        }
    }

    private void createEphemeral(String path, byte[] data) throws Exception {
        curator.create()
            .creatingParentsIfNeeded()
            .withMode(CreateMode.EPHEMERAL)
            .forPath(path, data);
        LOG.info("Created zk ephemeral path: {}", path);
    }

    private void upsertEphemeral(String path, byte[] data) throws Exception {
        try {
            createEphemeral(path, data);
        } catch (KeeperException.NodeExistsException e) {
            try {
                updateData(path, data);
            } catch (KeeperException.NoNodeException ignored) {
                createEphemeral(path, data);
            }
        }
    }

    private void deletePath(String path) throws Exception {
        try {
            curator.delete().guaranteed().deletingChildrenIfNeeded().forPath(path);
            LOG.info("Deleted zk path: {}", path);
        } catch (KeeperException.NoNodeException ignored) {
            // ignored
        }
    }

    private boolean existsPath(String path) throws Exception {
        return curator.checkExists().forPath(path) != null;
    }

    private void updateData(String path, byte[] data) throws Exception {
        curator.setData().forPath(path, data);
    }

    private byte[] getData(String path) throws Exception {
        try {
            return curator.getData().forPath(path);
        } catch (KeeperException.NoNodeException ignored) {
            return null;
        }
    }

    private void heartbeat() {
        RetryTemplate.executeQuietly(() -> {
            if (closed) {
                return;
            }
            byte[] workerIdData = getData(workerIdPath);
            if (workerIdData != null) {
                WorkerIdData data = WorkerIdData.deserialize(workerIdData);
                Assert.state(serverTag.equals(data.server), () -> "Inconsistent server tag: " + serverTag + " != " + data.server);
            }

            updateData(workerIdPath, WorkerIdData.of(System.currentTimeMillis(), serverTag).serialize());
        }, 3, 1000L);
    }

    private int registerWorkerId(int workerIdBitLength) throws Exception {
        int workerIdMaxCount = 1 << workerIdBitLength;
        byte[] serverTagData = getData(serverTagPath);

        // 判断当前serverTag是否已经注册
        if (serverTagData == null) {
            // 未注册

            // 捞取所有已注册的workerId
            Set<Integer> usedWorkIds = curator.getChildren()
                .forPath(serverTagParentPath)
                .stream()
                .map(e -> serverTagParentPath + SEP + e)
                .map(ThrowingFunction.toChecked(this::getData))
                .filter(Objects::nonNull)
                .map(Bytes::toInt)
                .collect(Collectors.toSet());
            List<Integer> usableWorkerIds = IntStream.range(0, workerIdMaxCount)
                .boxed()
                .filter(Predicates.not(usedWorkIds::contains))
                .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(usableWorkerIds)) {
                throw new IllegalStateException("Not found usable zk worker id.");
            }

            Collections.shuffle(usableWorkerIds);
            for (int usableWorkerId : usableWorkerIds) {
                String workerIdPath0 = workerIdParentPath + SEP + usableWorkerId;
                boolean isCreatedWorkerIdPath = false;
                long currentTime = System.currentTimeMillis();
                try {
                    WorkerIdData data = WorkerIdData.of(currentTime, serverTag);
                    createEphemeral(workerIdPath0, data.serialize());
                    isCreatedWorkerIdPath = true;
                    upsertEphemeral(serverTagPath, Bytes.toBytes(usableWorkerId));
                    LOG.info("Created snowflake zk worker success: {}, {}, {}", serverTag, usableWorkerId, currentTime);
                    return usableWorkerId;
                } catch (Throwable t) {
                    if (isCreatedWorkerIdPath) {
                        ThrowingRunnable.doCaught(() -> deletePath(workerIdPath0));
                    }
                    LOG.warn("Registry snowflake zk worker '{}' failed: {}", workerIdPath0, t.getMessage());
                    Threads.interruptIfNecessary(t);
                }
            }
            throw new IllegalStateException("Cannot found usable zk worker id: " + serverTagParentPath);

        } else {
            // 已注册

            int workerId0 = Bytes.toInt(serverTagData);
            if (workerId0 < 0 || workerId0 >= workerIdMaxCount) {
                deletePath(serverTagPath);
                throw new IllegalStateException("Invalid zk worker id: " + workerId0);
            }

            byte[] workerIdData = getData(workerIdPath);
            if (workerIdData == null) {
                WorkerIdData data = WorkerIdData.of(System.currentTimeMillis(), serverTag);
                upsertEphemeral(workerIdPath, data.serialize());
            } else {
                WorkerIdData data = WorkerIdData.deserialize(workerIdData);
                if (!serverTag.equals(data.server)) {
                    throw new IllegalStateException("Inconsistent server tag, actual=" + serverTag + ", obtain=" + data.server);
                }
                long currentTime = System.currentTimeMillis();
                if (currentTime < data.time) {
                    throw new ClockMovedBackwardsException(String.format("Clock moved backwards: %s, %s, %d", serverTagPath, currentTime, data.time));
                }
                updateData(workerIdPath, WorkerIdData.of(currentTime, serverTag).serialize());
            }

            LOG.info("Reuse zk worker id success: {}, {}", serverTag, workerId0);

            return workerId0;

        }
    }

    private void onReconnected() throws Exception {
        byte[] serverTagData = getData(serverTagPath);
        if (serverTagData == null) {
            createEphemeral(serverTagPath, Bytes.toBytes(workerId));
        } else {
            int id = Bytes.toInt(serverTagData);
            Assert.isTrue(id == workerId, () -> "Reconnected worker id was changed, expect=" + workerId + ", actual=" + id);
        }

        byte[] workerIdData = getData(workerIdPath);
        if (workerIdData == null) {
            createEphemeral(workerIdPath, WorkerIdData.of(System.currentTimeMillis(), serverTag).serialize());
        } else {
            WorkerIdData data = WorkerIdData.deserialize(workerIdData);
            Assert.isTrue(serverTag.equals(data.server), () -> "Reconnected server tag was changed, expect=" + serverTag + ", actual=" + data.server);
            updateData(workerIdPath, WorkerIdData.of(System.currentTimeMillis(), serverTag).serialize());
        }
    }

    private static CuratorFramework createCuratorFramework(ZkConfig config) throws InterruptedException {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(
            config.getBaseSleepTimeMs(),
            config.getMaxRetries(),
            config.getMaxSleepMs()
        );

        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
            .connectString(config.getConnectString())
            .connectionTimeoutMs(config.getConnectionTimeoutMs())
            .sessionTimeoutMs(config.getSessionTimeoutMs())
            .retryPolicy(retryPolicy);

        String authorization = config.authorization();
        if (authorization != null) {
            builder.authorization("digest", authorization.getBytes());
        }

        CuratorFramework curatorFramework = builder.build();

        curatorFramework.start();
        boolean isStarted = curatorFramework.getState().equals(CuratorFrameworkState.STARTED);
        Assert.state(isStarted, () -> "Snowflake curator framework not started: " + curatorFramework.getState());
        boolean isConnected = curatorFramework.blockUntilConnected(5000, TimeUnit.MILLISECONDS);
        Assert.state(isConnected, () -> "Snowflake curator framework not connected: " + curatorFramework.getState());
        return curatorFramework;
    }

    private class CuratorConnectionStateListener implements ConnectionStateListener {
        private static final long UNKNOWN_SESSION_ID = -1L;

        private final ZkDistributedSnowflake zkDistributedSnowflake;
        private long lastSessionId;

        public CuratorConnectionStateListener(ZkDistributedSnowflake zkDistributedSnowflake) {
            this.zkDistributedSnowflake = zkDistributedSnowflake;
        }

        @Override
        public void stateChanged(CuratorFramework client, ConnectionState state) {
            long sessionId;
            try {
                sessionId = client.getZookeeperClient().getZooKeeper().getSessionId();
            } catch (Throwable t) {
                sessionId = UNKNOWN_SESSION_ID;
                LOG.warn("Curator snowflake client state changed, get session instance error.", t);
                Threads.interruptIfNecessary(t);
            }
            if (state == ConnectionState.CONNECTED) {
                lastSessionId = sessionId;
                LOG.info("Curator snowflake first connected, session={}", sessionId);
            } else if (state == ConnectionState.LOST) {
                LOG.warn("Curator snowflake session expired, session={}", lastSessionId);
            } else if (state == ConnectionState.SUSPENDED) {
                LOG.warn("Curator snowflake connection lost, session={}", sessionId);
            } else if (state == ConnectionState.RECONNECTED) {
                if (lastSessionId == sessionId && sessionId != UNKNOWN_SESSION_ID) {
                    LOG.warn("Curator snowflake recover connected, reuse old-session={}", sessionId);
                } else {
                    LOG.warn("Curator snowflake recover connected, old-session={}, new-session={}", lastSessionId, sessionId);
                    lastSessionId = sessionId;
                }

                ThrowingRunnable.doCaught(() -> RetryTemplate.execute(zkDistributedSnowflake::onReconnected, 3, 1000));
            }
        }
    }

    private static class WorkerIdData {
        private final long time;
        private final String server;

        private WorkerIdData(long time, String server) {
            this.time = time;
            this.server = server;
        }

        private static WorkerIdData of(long time, String server) {
            return new WorkerIdData(time, server);
        }

        private byte[] serialize() {
            return ArrayUtils.addAll(Bytes.toBytes(time), server.getBytes(UTF_8));
        }

        private static WorkerIdData deserialize(byte[] bytes) {
            long time = Bytes.toLong(bytes, 0);
            String server = new String(bytes, 8, bytes.length - 8, UTF_8);
            return WorkerIdData.of(time, server);
        }
    }

}

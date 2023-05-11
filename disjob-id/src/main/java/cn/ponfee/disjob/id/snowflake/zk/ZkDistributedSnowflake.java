package cn.ponfee.disjob.id.snowflake.zk;

import cn.ponfee.disjob.common.base.IdGenerator;
import cn.ponfee.disjob.common.base.RetryTemplate;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingFunction;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingSupplier;
import cn.ponfee.disjob.common.util.Bytes;
import cn.ponfee.disjob.common.util.ObjectUtils;
import cn.ponfee.disjob.id.snowflake.ClockBackwardsException;
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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Snowflake server based zookeeper
 *
 * <pre>
 * /snowflake/{bizTag}
 * ├── tag
 * │   ├── serverTag-a   data=workerId-1
 * │   ├── serverTag-b   data=workerId-2
 * │   └── serverTag-c   data=workerId-3
 * └── id
 *     ├── workerId-1    data=lastHeartbeatTime
 *     ├── workerId-2    data=lastHeartbeatTime
 *     └── workerId-3    data=lastHeartbeatTime
 *
 * </pre>
 *
 * @author Ponfee
 */
public class ZkDistributedSnowflake implements IdGenerator, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ZkDistributedSnowflake.class);

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
     * /snowflake/{bizTag}/id
     */
    private final String workerIdParentPath;

    private final CuratorFramework curator;

    private final int workerId;

    private final Snowflake snowflake;

    private final ScheduledExecutorService heartbeatScheduler;

    public ZkDistributedSnowflake(ZookeeperConfig zkConfig, String bizTag, String serverTag) {
        this(zkConfig, bizTag, serverTag, 14, 8);
    }

    public ZkDistributedSnowflake(ZookeeperConfig zkConfig,
                                  String bizTag,
                                  String serverTag,
                                  int sequenceBitLength,
                                  int workerIdBitLength) {
        Assert.isTrue(!bizTag.contains(SEP), "Biz tag cannot contains '/': " + bizTag);
        Assert.isTrue(!serverTag.contains(SEP), "Server tag cannot contains '/': " + serverTag);
        this.serverTag = serverTag;
        String snowflakeRootPath = "/snowflake/" + bizTag;
        this.serverTagParentPath = snowflakeRootPath + "/tag";
        this.workerIdParentPath = snowflakeRootPath + "/id";

        try {
            this.curator = createCuratorFramework(zkConfig);
            RetryTemplate.execute(() -> createPersistent(snowflakeRootPath), 3, 1000L);
            RetryTemplate.execute(() -> createPersistent(serverTagParentPath), 3, 1000L);
            RetryTemplate.execute(() -> createPersistent(workerIdParentPath), 3, 1000L);
        } catch (Throwable e) {
            throw new Error("Zk snowflake server initialize error.", e);
        }

        try {
            this.workerId = RetryTemplate.execute(() -> registryWorkerId(workerIdBitLength), 5, 2000L);
            this.snowflake = new Snowflake(workerId, sequenceBitLength, workerIdBitLength);
        } catch (Throwable e) {
            throw new Error("Zk snowflake server registry worker error.", e);
        }

        curator.getConnectionStateListenable().addListener(new CuratorConnectionStateListener(this));

        this.heartbeatScheduler = new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = new Thread(r, "zk_snowflake_worker_heartbeat");
            thread.setDaemon(true);
            thread.setPriority(Thread.MAX_PRIORITY);
            return thread;
        });
        heartbeatScheduler.scheduleWithFixedDelay(this::heartbeat, 1, 3, TimeUnit.SECONDS);
    }

    @Override
    public long generateId() {
        return snowflake.generateId();
    }

    @Override
    public void close() {
        ThrowingSupplier.caught(() -> ThreadPoolExecutors.shutdown(heartbeatScheduler, 3));
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
            updateData(path, data);
        }
    }

    private void deletePath(String path) throws Exception {
        try {
            curator.delete().guaranteed().deletingChildrenIfNeeded().forPath(path);
            LOG.info("Deleted zk path: {}", path);
        } catch (KeeperException.NoNodeException ignored) {
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
        String workerIdPath = workerIdParentPath + SEP + workerId;
        try {
            RetryTemplate.execute(() -> {
                byte[] workerIdData = getData(workerIdPath);
                if (workerIdData != null) {
                    WorkerIdData data = WorkerIdData.deserialize(workerIdData);
                    Assert.state(serverTag.equals(data.server), "Inconsistent server tag: " + serverTag + " != " + data.server);
                }

                updateData(workerIdPath, WorkerIdData.of(System.currentTimeMillis(), serverTag).serialize());
            }, 3, 1000L);
        } catch (Throwable e) {
            LOG.error("Zk snowflake server heartbeat error: " + workerIdPath, e);
        }
    }

    private int registryWorkerId(int workerIdBitLength) throws Exception {
        int workerIdMaxCount = 1 << workerIdBitLength;
        String serverTagPath = serverTagParentPath + SEP + serverTag;
        byte[] serverTagData = getData(serverTagPath);

        // 判断当前serverTag是否已经注册
        if (serverTagData == null) {
            // 不存在

            // 捞取所有已注册的workerId
            Set<Integer> usedWorkIds = curator.getChildren()
                .forPath(serverTagParentPath)
                .stream()
                .map(e -> serverTagParentPath + SEP + e)
                .map(ThrowingFunction.checked(this::getData))
                .filter(Objects::nonNull)
                .map(Bytes::toInt)
                .collect(Collectors.toSet());
            List<Integer> usableWorkerIds = IntStream.range(0, workerIdMaxCount)
                .boxed()
                .filter(ObjectUtils.not(usedWorkIds::contains))
                .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(usableWorkerIds)) {
                throw new IllegalStateException("Not found usable zk worker id.");
            }

            Collections.shuffle(usableWorkerIds);
            for (int usableWorkerId : usableWorkerIds) {
                String workerIdPath = workerIdParentPath + SEP + usableWorkerId;
                boolean isCreatedWorkerIdPath = false;
                long currentTime = System.currentTimeMillis();
                try {
                    WorkerIdData data = WorkerIdData.of(currentTime, serverTag);
                    createEphemeral(workerIdPath, data.serialize());
                    isCreatedWorkerIdPath = true;
                    upsertEphemeral(serverTagPath, Bytes.toBytes(usableWorkerId));
                    LOG.info("Created snowflake zk worker success: {} | {} | {}", serverTag, usableWorkerId, currentTime);
                    return usableWorkerId;
                } catch (Throwable t) {
                    if (isCreatedWorkerIdPath) {
                        ThrowingRunnable.caught(() -> deletePath(workerIdPath));
                    }
                    LOG.warn("Registry snowflake zk worker '{}' failed: {}", workerIdPath, t.getMessage());
                }
            }
            throw new IllegalStateException("Cannot found usable zk worker id: " + serverTagParentPath);

        } else {
            // 已存在

            int workerId = Bytes.toInt(serverTagData);
            if (workerId < 0 || workerId >= workerIdMaxCount) {
                deletePath(serverTagPath);
                throw new IllegalStateException("Invalid zk worker id: " + workerId);
            }

            String workerIdPath = workerIdParentPath + SEP + workerId;
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
                    throw new ClockBackwardsException(String.format("Clock moved backwards: %s | %s | %d", serverTagPath, currentTime, data.time));
                }
                updateData(workerIdPath, WorkerIdData.of(currentTime, serverTag).serialize());
            }

            LOG.info("Reuse zk worker id success: {} | {}", serverTag, workerId);

            return workerId;

        }
    }

    private void onReconnected() throws Exception {
        String serverTagPath = serverTagParentPath + SEP + serverTag;
        byte[] serverTagData = getData(serverTagPath);
        if (serverTagData == null) {
            createEphemeral(serverTagPath, Bytes.toBytes(workerId));
        } else {
            int id = Bytes.toInt(serverTagData);
            Assert.isTrue(id == workerId, "Reconnected worker id was changed, expect=" + workerId + ", actual=" + id);
        }

        String workerIdPath = workerIdParentPath + SEP + workerId;
        byte[] workerIdData = getData(workerIdPath);
        if (workerIdData == null) {
            createEphemeral(workerIdPath, WorkerIdData.of(System.currentTimeMillis(), serverTag).serialize());
        } else {
            WorkerIdData data = WorkerIdData.deserialize(workerIdData);
            Assert.isTrue(serverTag.equals(data.server), "Reconnected server tag was changed, expect=" + serverTag + ", actual=" + data.server);
            updateData(workerIdPath, WorkerIdData.of(System.currentTimeMillis(), serverTag).serialize());
        }
    }

    private static CuratorFramework createCuratorFramework(ZookeeperConfig config) throws InterruptedException {
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

    private static class CuratorConnectionStateListener implements ConnectionStateListener {
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
            } catch (Exception e) {
                sessionId = UNKNOWN_SESSION_ID;
                LOG.warn("Curator snowflake client state changed, get session instance error.", e);
            }
            if (state == ConnectionState.CONNECTED) {
                lastSessionId = sessionId;
                LOG.info("Curator snowflake first connected, session={}", hex(sessionId));
            } else if (state == ConnectionState.LOST) {
                LOG.warn("Curator snowflake session expired, session={}", hex(lastSessionId));
            } else if (state == ConnectionState.SUSPENDED) {
                LOG.warn("Curator snowflake connection lost, session={}", hex(sessionId));
            } else if (state == ConnectionState.RECONNECTED) {
                if (lastSessionId == sessionId && sessionId != UNKNOWN_SESSION_ID) {
                    LOG.warn("Curator snowflake recover connected, reuse old-session={}", hex(sessionId));
                } else {
                    LOG.warn("Curator snowflake recover connected, old-session={}, new-session={}", hex(lastSessionId), hex(sessionId));
                    lastSessionId = sessionId;
                }

                ThrowingRunnable.caught(() -> RetryTemplate.execute(zkDistributedSnowflake::onReconnected, 3, 1000));
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

    private static String hex(long number) {
        return Long.toHexString(number);
    }

}

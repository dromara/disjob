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
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
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

/**
 * Snowflake server based zookeeper
 *
 * <pre>
 * /snowflake/{bizTag}
 * ├── tag
 * │   ├── bizTag-a      data=workerId-1
 * │   ├── bizTag-b      data=workerId-2
 * │   └── bizTag-c      data=workerId-3
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
            String res = curator.create().creatingParentsIfNeeded().forPath(path);
            LOG.info("Created zk persistent path '{}' result '{}'", path, res);
        } catch (KeeperException.NodeExistsException ignored) {
            // ignored
        }
    }

    private void createEphemeral(String path, byte[] data) throws Exception {
        String res = curator.create()
            .creatingParentsIfNeeded()
            .withMode(CreateMode.EPHEMERAL)
            .forPath(path, data);
        LOG.info("Created zk ephemeral path '{}' result '{}'", path, res);
    }

    private void deletePath(String path) throws Exception {
        try {
            curator.delete()/*.guaranteed()*/.deletingChildrenIfNeeded().forPath(path);
        } catch (KeeperException.NoNodeException ignored) {
        }
    }

    private boolean existsPath(String path) throws Exception {
        return curator.checkExists().forPath(path) != null;
    }

    private byte[] getData(String path) throws Exception {
        try {
            return curator.getData().forPath(path);
        } catch (KeeperException.NoNodeException ignored) {
            return null;
        }
    }

    private void updateData(String path, byte[] data) throws Exception {
        curator.setData().forPath(path, data);
    }

    private void heartbeat() {
        String workerIdPath = workerIdParentPath + SEP + workerId;
        try {
            RetryTemplate.execute(() -> updateData(workerIdPath, Bytes.toBytes(System.currentTimeMillis())), 3, 1000L);
        } catch (Throwable e) {
            LOG.error("Zk snowflake server heartbeat error: " + workerIdPath, e);
        }
    }

    private int registryWorkerId(int workerIdBitLength) throws Exception {
        int workerIdMaxCount = 1 << workerIdBitLength;
        String serverTagPath = serverTagParentPath + SEP + serverTag;
        byte[] workerIdData = getData(serverTagPath);

        // 判断当前serverTag是否已经注册
        if (workerIdData == null) {
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

            if (!existsPath(serverTagPath)) {
                createEphemeral(serverTagPath, null);
            }

            Collections.shuffle(usableWorkerIds);
            for (int usableWorkerId : usableWorkerIds) {
                String workerIdPath = workerIdParentPath + SEP + usableWorkerId;
                long currentTime = System.currentTimeMillis();
                boolean isCreatedWorkerIdPath = false;
                try {
                    createEphemeral(workerIdPath, Bytes.toBytes(currentTime));
                    isCreatedWorkerIdPath = true;
                    updateData(serverTagPath, Bytes.toBytes(usableWorkerId));
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

            int workerId = Bytes.toInt(workerIdData);
            if (workerId < 0 || workerId >= workerIdMaxCount) {
                deletePath(serverTagPath);
                throw new IllegalStateException("Invalid zk worker id: " + workerId);
            }

            String workerIdPath = workerIdParentPath + SEP + workerId;
            byte[] heartbeatTimeData = getData(workerIdPath);
            if (heartbeatTimeData == null) {
                createEphemeral(workerIdPath, Bytes.toBytes(System.currentTimeMillis()));
                if ((heartbeatTimeData = getData(workerIdPath)) == null) {
                    throw new IllegalStateException("Not found zk worker id: " + workerId);
                }
            }

            long currentTime = System.currentTimeMillis();
            long lastHeartbeatTime = Bytes.toLong(heartbeatTimeData);
            if (currentTime < lastHeartbeatTime) {
                throw new ClockBackwardsException(String.format("Clock moved backwards: %s | %s | %d", serverTagPath, currentTime, lastHeartbeatTime));
            }

            updateData(workerIdPath, Bytes.toBytes(currentTime));
            LOG.info("Reuse zk worker id success: {} | {}", serverTag, workerId);

            return workerId;

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

}

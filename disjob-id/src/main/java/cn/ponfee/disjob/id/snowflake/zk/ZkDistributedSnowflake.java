/*
 * Copyright 2022-2026 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.id.snowflake.zk;

import cn.ponfee.disjob.common.base.IdGenerator;
import cn.ponfee.disjob.common.base.RetryTemplate;
import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.exception.Throwables;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingFunction;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.util.Bytes;
import cn.ponfee.disjob.id.snowflake.ClockMovedBackwardsException;
import cn.ponfee.disjob.id.snowflake.Snowflake;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
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

import static cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors.commonScheduledPool;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Snowflake configuration based zookeeper
 *
 * <pre>
 * /snowflake/{bizName}
 * ├── name (children are EPHEMERAL node)
 * │   ├── serverName-a   data=workerId-1
 * │   ├── serverName-b   data=workerId-2
 * │   └── serverName-c   data=workerId-3
 * └── id (children are EPHEMERAL node)
 *     ├── workerId-1    data=lastHeartbeatTime + serverName
 *     ├── workerId-2    data=lastHeartbeatTime + serverName
 *     └── workerId-3    data=lastHeartbeatTime + serverName
 *
 * </pre>
 *
 * @author Ponfee
 */
@Slf4j
public class ZkDistributedSnowflake extends SingletonClassConstraint implements IdGenerator, Closeable {

    private static final long HEARTBEAT_PERIOD_MS = 30_000L;

    private static final String SEP = "/";

    /**
     * Zookeeper client
     */
    private final CuratorFramework curatorFramework;

    /**
     * Server name
     */
    private final String serverName;

    /**
     * /snowflake/{bizName}/name
     */
    private final String serverNameParentPath;

    /**
     * /snowflake/{bizName}/name/{serverName}
     */
    private final String serverNamePath;

    /**
     * /snowflake/{bizName}/id
     */
    private final String workerIdParentPath;

    /**
     * /snowflake/{bizName}/id/{workerId}
     */
    private final String workerIdPath;

    /**
     * Assigned worker id
     */
    private final int workerId;

    /**
     * Snowflake
     */
    private final Snowflake snowflake;

    private volatile boolean closed = false;

    public ZkDistributedSnowflake(CuratorFramework curatorFramework, String bizName, String serverName) {
        this(curatorFramework, bizName, serverName, 8, 14);
    }

    public ZkDistributedSnowflake(CuratorFramework curatorFramework,
                                  String bizName,
                                  String serverName,
                                  int workerIdBitLength,
                                  int sequenceBitLength) {
        Assert.isTrue(!bizName.contains(SEP), () -> "Biz name cannot contains '/': " + bizName);
        Assert.isTrue(!serverName.contains(SEP), () -> "Server name cannot contains '/': " + serverName);
        int len = workerIdBitLength + sequenceBitLength;
        Assert.isTrue(len <= 22, () -> "Bit length(sequence + worker) cannot greater than 22, but actual=" + len);
        this.curatorFramework = curatorFramework;
        this.serverName = serverName;
        String snowflakeRootPath = "/snowflake/" + bizName;
        this.serverNameParentPath = snowflakeRootPath + "/name";
        this.workerIdParentPath = snowflakeRootPath + "/id";
        this.serverNamePath = serverNameParentPath + SEP + serverName;

        RetryTemplate.execute(() -> createPersistent(snowflakeRootPath), 3, 1000L);
        RetryTemplate.execute(() -> createPersistent(serverNameParentPath), 3, 1000L);
        RetryTemplate.execute(() -> createPersistent(workerIdParentPath), 3, 1000L);

        // workerId取值范围：[0, workerIdMaxCount)
        int workerIdMaxCount = 1 << workerIdBitLength;
        this.workerId = RetryTemplate.execute(() -> registerWorkerId(workerIdMaxCount), 5, 2000L);
        this.workerIdPath = workerIdParentPath + SEP + workerId;
        this.snowflake = new Snowflake(workerIdBitLength, sequenceBitLength, workerId);

        curatorFramework.getConnectionStateListenable().addListener(new CuratorConnectionStateListener(this));

        commonScheduledPool().scheduleWithFixedDelay(this::heartbeat, 0, HEARTBEAT_PERIOD_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public long generateId() {
        return snowflake.generateId();
    }

    @PreDestroy
    @Override
    public void close() {
        closed = true;
        ThrowingRunnable.doCaught(curatorFramework::close);
    }

    // ------------------------------------------------------------------private methods

    private void createPersistent(String path) throws Exception {
        try {
            curatorFramework.create().creatingParentsIfNeeded().forPath(path);
            log.info("Created zk persistent path: {}", path);
        } catch (KeeperException.NodeExistsException ignored) {
            // ignored
        }
    }

    private void createEphemeral(String path, byte[] data) throws Exception {
        curatorFramework.create()
            .creatingParentsIfNeeded()
            .withMode(CreateMode.EPHEMERAL)
            .forPath(path, data);
        log.info("Created zk ephemeral path: {}", path);
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
            curatorFramework.delete().guaranteed().deletingChildrenIfNeeded().forPath(path);
            log.info("Deleted zk path: {}", path);
        } catch (KeeperException.NoNodeException ignored) {
            // ignored
        }
    }

    private boolean existsPath(String path) throws Exception {
        return curatorFramework.checkExists().forPath(path) != null;
    }

    private void updateData(String path, byte[] data) throws Exception {
        curatorFramework.setData().forPath(path, data);
    }

    private byte[] getData(String path) throws Exception {
        try {
            return curatorFramework.getData().forPath(path);
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
                Assert.state(serverName.equals(data.server), () -> "Inconsistent server name: " + serverName + " != " + data.server);
            }

            updateData(workerIdPath, WorkerIdData.of(System.currentTimeMillis(), serverName).serialize());
        }, 3, 2000L);
    }

    private int registerWorkerId(int workerIdMaxCount) throws Exception {
        byte[] serverNameData = getData(serverNamePath);
        // 判断当前serverName是否已经注册
        if (ArrayUtils.isEmpty(serverNameData)) {
            // 未注册
            return findUsableWorkerId(workerIdMaxCount);
        } else {
            // 已注册
            return reuseWorkerId(serverNameData, workerIdMaxCount);
        }
    }

    private int findUsableWorkerId(int workerIdMaxCount) throws Exception {
        // 捞取所有已注册的workerId
        Set<Integer> usedWorkerIds = curatorFramework.getChildren()
            .forPath(serverNameParentPath)
            .stream()
            .map(e -> serverNameParentPath + SEP + e)
            .map(ThrowingFunction.toChecked(this::getData))
            .filter(Objects::nonNull)
            .map(Bytes::toInt)
            .collect(Collectors.toSet());
        List<Integer> usableWorkerIds = IntStream.range(0, workerIdMaxCount)
            .boxed()
            .filter(e -> !usedWorkerIds.contains(e))
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
                WorkerIdData data = WorkerIdData.of(currentTime, serverName);
                // create worker id ephemeral node: /snowflake/{bizName}/id/{workerId}
                createEphemeral(workerIdPath0, data.serialize());
                isCreatedWorkerIdPath = true;
                // create server name ephemeral node: /snowflake/{bizName}/name/{serverName}
                upsertEphemeral(serverNamePath, Bytes.toBytes(usableWorkerId));
                log.info("Created snowflake zk worker success: {}, {}, {}", serverName, usableWorkerId, currentTime);
                return usableWorkerId;
            } catch (Throwable t) {
                log.warn("Registry snowflake zk worker '{}' failed: {}", workerIdPath0, t.getMessage());
                if (isCreatedWorkerIdPath) {
                    ThrowingRunnable.doCaught(() -> deletePath(workerIdPath0));
                }
                ExceptionUtils.rethrow(t);
            }
        }
        throw new IllegalStateException("Cannot found usable zk worker id: " + serverNameParentPath);
    }

    private int reuseWorkerId(byte[] serverNameData, int workerIdMaxCount) throws Exception {
        int currentWorkerId = Bytes.toInt(serverNameData);
        if (currentWorkerId < 0 || currentWorkerId >= workerIdMaxCount) {
            deletePath(serverNamePath);
            throw new IllegalStateException("Invalid zk worker id: " + currentWorkerId);
        }

        byte[] workerIdData = getData(workerIdPath);
        if (workerIdData == null) {
            WorkerIdData data = WorkerIdData.of(System.currentTimeMillis(), serverName);
            upsertEphemeral(workerIdPath, data.serialize());
        } else {
            WorkerIdData data = WorkerIdData.deserialize(workerIdData);
            if (!serverName.equals(data.server)) {
                throw new IllegalStateException("Inconsistent server name, actual=" + serverName + ", obtain=" + data.server);
            }
            long currentTime = System.currentTimeMillis();
            if (currentTime < data.time) {
                throw new ClockMovedBackwardsException(String.format("Clock moved backwards: %s, %s, %d", serverNamePath, currentTime, data.time));
            }
            updateData(workerIdPath, WorkerIdData.of(currentTime, serverName).serialize());
        }

        log.info("Reuse zk worker id success: {}, {}", serverName, currentWorkerId);

        return currentWorkerId;
    }

    private void onReconnected() throws Exception {
        byte[] serverNameData = getData(serverNamePath);
        if (serverNameData == null) {
            createEphemeral(serverNamePath, Bytes.toBytes(workerId));
        } else {
            int id = Bytes.toInt(serverNameData);
            Assert.isTrue(id == workerId, () -> "Reconnected worker id was changed, expect=" + workerId + ", actual=" + id);
        }

        byte[] workerIdData = getData(workerIdPath);
        if (workerIdData == null) {
            createEphemeral(workerIdPath, WorkerIdData.of(System.currentTimeMillis(), serverName).serialize());
        } else {
            WorkerIdData data = WorkerIdData.deserialize(workerIdData);
            Assert.isTrue(serverName.equals(data.server), () -> "Reconnected server name was changed, expect=" + serverName + ", actual=" + data.server);
            updateData(workerIdPath, WorkerIdData.of(System.currentTimeMillis(), serverName).serialize());
        }
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
            } catch (Throwable t) {
                sessionId = UNKNOWN_SESSION_ID;
                log.warn("Curator snowflake client state changed, get session instance error.", t);
                Throwables.rethrowIfFatal(t);
            }
            if (state == ConnectionState.CONNECTED) {
                lastSessionId = sessionId;
                log.info("Curator snowflake first connected, session={}", sessionId);
            } else if (state == ConnectionState.LOST) {
                log.warn("Curator snowflake session expired, session={}", lastSessionId);
            } else if (state == ConnectionState.SUSPENDED) {
                log.warn("Curator snowflake connection lost, session={}", sessionId);
            } else if (state == ConnectionState.RECONNECTED) {
                if (lastSessionId == sessionId && sessionId != UNKNOWN_SESSION_ID) {
                    log.warn("Curator snowflake recover connected, reuse old-session={}", sessionId);
                } else {
                    log.warn("Curator snowflake recover connected, old-session={}, new-session={}", lastSessionId, sessionId);
                    lastSessionId = sessionId;
                }

                RetryTemplate.executeQuietly(zkDistributedSnowflake::onReconnected, 3, 1000);
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

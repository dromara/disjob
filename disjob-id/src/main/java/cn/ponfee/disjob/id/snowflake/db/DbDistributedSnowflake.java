/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
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

package cn.ponfee.disjob.id.snowflake.db;

import cn.ponfee.disjob.common.base.IdGenerator;
import cn.ponfee.disjob.common.base.RetryTemplate;
import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.concurrent.Threads;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingSupplier;
import cn.ponfee.disjob.common.spring.JdbcTemplateWrapper;
import cn.ponfee.disjob.common.util.Predicates;
import cn.ponfee.disjob.id.snowflake.ClockMovedBackwardsException;
import cn.ponfee.disjob.id.snowflake.Snowflake;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;

import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors.commonScheduledPool;
import static cn.ponfee.disjob.common.spring.JdbcTemplateWrapper.AFFECTED_ONE_ROW;

/**
 * Snowflake server based database
 *
 * @author Ponfee
 */
public class DbDistributedSnowflake extends SingletonClassConstraint implements IdGenerator, Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(DbDistributedSnowflake.class);

    private static final long EXPIRE_TIME_MILLIS = TimeUnit.HOURS.toMillis(12);

    private static final long HEARTBEAT_PERIOD_MS = 60000;

    private static final RowMapper<DbSnowflakeWorker> ROW_MAPPER = new BeanPropertyRowMapper<>(DbSnowflakeWorker.class);

    private static final String TABLE_NAME = "snowflake_worker";

    private static final String CREATE_TABLE_DDL =
        "CREATE TABLE IF NOT EXISTS `" + TABLE_NAME + "` (                                                                    \n" +
        "  `id`              BIGINT        UNSIGNED  NOT NULL  AUTO_INCREMENT  COMMENT 'auto increment id',                   \n" +
        "  `biz_tag`         VARCHAR(60)             NOT NULL                  COMMENT 'biz tag',                             \n" +
        "  `server_tag`      VARCHAR(128)            NOT NULL                  COMMENT 'server tag, for example ip:port',     \n" +
        "  `worker_id`       INT           UNSIGNED  NOT NULL                  COMMENT 'snowflake worker-id',                 \n" +
        "  `heartbeat_time`  BIGINT        UNSIGNED  NOT NULL                  COMMENT 'last heartbeat time',                 \n" +
        "  PRIMARY KEY (`id`),                                                                                                \n" +
        "  UNIQUE KEY `uk_biztag_servertag` (`biz_tag`, `server_tag`),                                                        \n" +
        "  UNIQUE KEY `uk_biztag_workerid` (`biz_tag`, `worker_id`)                                                           \n" +
        ") ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Allocate snowflake worker-id'; \n" ;

    private static final String QUERY_ALL_SQL = "SELECT biz_tag, server_tag, worker_id, heartbeat_time FROM " + TABLE_NAME + " WHERE biz_tag=?";

    private static final String GET_WORKER_SQL = "SELECT biz_tag, server_tag, worker_id, heartbeat_time FROM " + TABLE_NAME + " WHERE biz_tag=? AND server_tag=?";

    private static final String REMOVE_DEAD_SQL = "DELETE FROM " + TABLE_NAME + " WHERE biz_tag=? AND heartbeat_time<?";

    private static final String REMOVE_INVALID_SQL = "DELETE FROM " + TABLE_NAME + " WHERE biz_tag=? AND server_tag=?";

    private static final String REGISTER_WORKER_SQL = "INSERT INTO " + TABLE_NAME + " (biz_tag, server_tag, worker_id, heartbeat_time) VALUES (?, ?, ?, ?)";

    private static final String DEREGISTER_WORKER_SQL = "DELETE FROM " + TABLE_NAME + " WHERE biz_tag=? AND server_tag=?";

    private static final String REUSE_WORKER_SQL = "UPDATE " + TABLE_NAME + " SET heartbeat_time=? WHERE biz_tag=? AND server_tag=? AND heartbeat_time=?";

    private static final String HEARTBEAT_WORKER_SQL = "UPDATE " + TABLE_NAME + " SET heartbeat_time=? WHERE biz_tag=? AND server_tag=?";

    private final JdbcTemplateWrapper jdbcTemplateWrapper;
    private final String bizTag;
    private final String serverTag;
    private final Snowflake snowflake;
    private volatile boolean closed = false;

    public DbDistributedSnowflake(JdbcTemplate jdbcTemplate, String bizTag, String serverTag) {
        this(jdbcTemplate, bizTag, serverTag, 14, 8);
    }

    public DbDistributedSnowflake(JdbcTemplate jdbcTemplate,
                                  String bizTag,
                                  String serverTag,
                                  int sequenceBitLength,
                                  int workerIdBitLength) {
        int len = sequenceBitLength + workerIdBitLength;
        Assert.isTrue(len <= 22, () -> "Bit length(sequence + worker) cannot greater than 22, but actual=" + len);

        this.jdbcTemplateWrapper = JdbcTemplateWrapper.of(jdbcTemplate);
        this.bizTag = bizTag;
        this.serverTag = serverTag;

        // create table
        jdbcTemplateWrapper.createTableIfNotExists(TABLE_NAME, CREATE_TABLE_DDL);

        try {
            // workerId取值范围：[0, workerIdMaxCount)
            int workerIdMaxCount = 1 << workerIdBitLength;
            int workerId = RetryTemplate.execute(() -> registerWorkerId(workerIdMaxCount), 5, 2000L);
            this.snowflake = new Snowflake(workerId, sequenceBitLength, workerIdBitLength);
        } catch (Throwable e) {
            Threads.interruptIfNecessary(e);
            throw new Error("Db snowflake server initialize error.", e);
        }

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
        ThrowingSupplier.doCaught(() -> jdbcTemplateWrapper.delete(DEREGISTER_WORKER_SQL, bizTag, serverTag));
    }

    // -------------------------------------------------------private methods

    private int registerWorkerId(int workerIdMaxCount) {
        List<DbSnowflakeWorker> registeredWorkers = jdbcTemplateWrapper.list(QUERY_ALL_SQL, ROW_MAPPER, bizTag);
        DbSnowflakeWorker current = registeredWorkers.stream().filter(e -> e.equals(bizTag, serverTag)).findAny().orElse(null);
        if (current == null) {
            return findUsableWorkerId(registeredWorkers, workerIdMaxCount);
        } else {
            return reuseWorkerId(current, workerIdMaxCount);
        }
    }

    private int findUsableWorkerId(List<DbSnowflakeWorker> registeredWorkers, int workerIdMaxCount) {
        if (registeredWorkers.size() > (workerIdMaxCount / 2)) {
            long oldestTimeMillis = System.currentTimeMillis() - EXPIRE_TIME_MILLIS;
            jdbcTemplateWrapper.delete(REMOVE_DEAD_SQL, bizTag, oldestTimeMillis);
            // re-query
            registeredWorkers = jdbcTemplateWrapper.list(QUERY_ALL_SQL, ROW_MAPPER, bizTag);
        }

        Set<Integer> usedWorkIds = registeredWorkers.stream().map(DbSnowflakeWorker::getWorkerId).collect(Collectors.toSet());
        List<Integer> usableWorkerIds = IntStream.range(0, workerIdMaxCount)
            .boxed()
            .filter(Predicates.not(usedWorkIds::contains))
            .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(usableWorkerIds)) {
            throw new IllegalStateException("Not found usable db worker id.");
        }

        Collections.shuffle(usableWorkerIds);
        for (Integer usableWorkerId : usableWorkerIds) {
            Object[] args = {bizTag, serverTag, usableWorkerId, System.currentTimeMillis()};
            try {
                jdbcTemplateWrapper.insert(REGISTER_WORKER_SQL, args);
                LOG.info("Create snowflake db worker success: {}, {}, {}, {}", args);
                return usableWorkerId;
            } catch (DuplicateKeyException ignored) {
                DbSnowflakeWorker existed = jdbcTemplateWrapper.get(GET_WORKER_SQL, ROW_MAPPER, bizTag, serverTag);
                if (existed != null) {
                    LOG.warn("Server tag duplicated: {}", existed);
                    return reuseWorkerId(existed, workerIdMaxCount);
                }
            }
        }

        throw new IllegalStateException("Cannot found usable db worker id: " + bizTag + ", " + serverTag);
    }

    private int reuseWorkerId(DbSnowflakeWorker current, int workerIdMaxCount) {
        Integer workerId = current.getWorkerId();
        if (workerId < 0 || workerId >= workerIdMaxCount) {
            if (jdbcTemplateWrapper.delete(REMOVE_INVALID_SQL, bizTag, serverTag) != AFFECTED_ONE_ROW) {
                LOG.error("Deleting invalid db worker id failed.");
            }
            throw new IllegalStateException("Invalid db worker id: " + workerId);
        }

        long currentTime = System.currentTimeMillis();
        long lastHeartbeatTime = current.getHeartbeatTime();
        if (currentTime < lastHeartbeatTime) {
            throw new ClockMovedBackwardsException(String.format("Clock moved backwards: %s, %s, %d, %d", bizTag, serverTag, currentTime, lastHeartbeatTime));
        }
        Object[] args = {currentTime, bizTag, serverTag, lastHeartbeatTime};
        if (jdbcTemplateWrapper.update(REUSE_WORKER_SQL, args) == AFFECTED_ONE_ROW) {
            LOG.info("Reuse db worker id success: {}, {}, {}, {}", args);
            return workerId;
        }

        throw new IllegalStateException("Reuse db worker id failed: " + bizTag + ", " + serverTag);
    }

    private void heartbeat() {
        RetryTemplate.executeQuietly(() -> {
            if (closed) {
                return;
            }
            Object[] args = {System.currentTimeMillis(), bizTag, serverTag};
            if (jdbcTemplateWrapper.update(HEARTBEAT_WORKER_SQL, args) == AFFECTED_ONE_ROW) {
                LOG.debug("Heartbeat db worker id success: {}, {}, {}", args);
            } else {
                LOG.error("Heartbeat db worker id failed: {}, {}, {}", args);
            }
        }, 5, 3000L);
    }

}

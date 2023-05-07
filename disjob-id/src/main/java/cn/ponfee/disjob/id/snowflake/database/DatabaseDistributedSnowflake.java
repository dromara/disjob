/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.id.snowflake.database;

import cn.ponfee.disjob.common.base.IdGenerator;
import cn.ponfee.disjob.common.base.RetryTemplate;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.exception.Throwables;
import cn.ponfee.disjob.common.util.ObjectUtils;
import cn.ponfee.disjob.id.snowflake.ClockBackwardsException;
import cn.ponfee.disjob.id.snowflake.Snowflake;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Snowflake server based database
 *
 * @author Ponfee
 */
public class DatabaseDistributedSnowflake implements IdGenerator, AutoCloseable {

    private final static Logger LOG = LoggerFactory.getLogger(DatabaseDistributedSnowflake.class);

    private static final RowMapper<SnowflakeWorker> ROW_MAPPER = new BeanPropertyRowMapper<>(SnowflakeWorker.class);

    private static final int AFFECTED_ONE_ROW = 1;

    private static final String TABLE_NAME = "snowflake_worker";

    private static final String CREATE_TABLE_SQL =
        "CREATE TABLE IF NOT EXISTS `" + TABLE_NAME + "` (                                                                                            \n" +
        "    `id`             bigint        unsigned  NOT NULL AUTO_INCREMENT                                        COMMENT 'auto increment id',     \n" +
        "    `biz_tag`        varchar(60)             NOT NULL                                                       COMMENT 'biz tag',               \n" +
        "    `server_tag`     varchar(128)            NOT NULL                                                       COMMENT 'server tag(ip:port)',   \n" +
        "    `worker_id`      int           unsigned  NOT NULL                                                       COMMENT 'snowflake worker-id',   \n" +
        "    `heartbeat_time` bigint        unsigned  NOT NULL                                                       COMMENT 'last heartbeat time',   \n" +
        "    `updated_at`     datetime(3)             NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'last updated time',     \n" +
        "    `created_at`     datetime(3)             NOT NULL DEFAULT CURRENT_TIMESTAMP                             COMMENT 'created time',          \n" +
        "    PRIMARY KEY (`id`),                                                                                                                      \n" +
        "    UNIQUE KEY `uk_biztag_servertag` (`biz_tag`, `server_tag`),                                                                              \n" +
        "    UNIQUE KEY `uk_biztag_workerid` (`biz_tag`, `worker_id`),                                                                                \n" +
        "    KEY `ix_createdat` (`created_at`),                                                                                                       \n" +
        "    KEY `ix_updatedat` (`updated_at`)                                                                                                        \n" +
        ") ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Allocate snowflake worker-id';                         \n" ;

    private static final String QUERY_ALL_SQL = "SELECT biz_tag, server_tag, worker_id, heartbeat_time FROM " + TABLE_NAME + " WHERE biz_tag=?";

    private static final String REMOVE_DEAD_SQL = "DELETE FROM " + TABLE_NAME + " WHERE biz_tag=? AND server_tag=? AND heartbeat_time=?";

    private static final String REGISTER_WORKER_SQL = "INSERT INTO " + TABLE_NAME + " (biz_tag, server_tag, worker_id, heartbeat_time) VALUES (?, ?, ?, ?)";

    private static final String DEREGISTER_WORKER_SQL = "DELETE FROM " + TABLE_NAME + " WHERE biz_tag=? AND server_tag=?";

    private static final String REUSE_WORKER_SQL = "UPDATE " + TABLE_NAME + " SET heartbeat_time=? WHERE biz_tag=? AND server_tag=? AND heartbeat_time=?";

    private static final String HEARTBEAT_WORKER_SQL = "UPDATE " + TABLE_NAME + " SET heartbeat_time=? WHERE biz_tag=? AND server_tag=?";

    private final JdbcTemplate jdbcTemplate;
    private final String bizTag;
    private final String serverTag;
    private final ScheduledExecutorService heartbeatScheduler;
    private final Snowflake snowflake;

    public DatabaseDistributedSnowflake(JdbcTemplate jdbcTemplate, String bizTag, String serverTag) {
        this(jdbcTemplate, bizTag, serverTag, 14, 8);
    }

    public DatabaseDistributedSnowflake(JdbcTemplate jdbcTemplate,
                                        String bizTag,
                                        String serverTag,
                                        int sequenceBitLength,
                                        int workerIdBitLength) {
        this.jdbcTemplate = jdbcTemplate;
        this.bizTag = bizTag;
        this.serverTag = serverTag;

        createTableIfNotExists();

        try {
            int workerId = RetryTemplate.execute(() -> registryWorkerId(workerIdBitLength), 5, 1000L);
            this.snowflake = new Snowflake(workerId, sequenceBitLength, workerIdBitLength);
        } catch (Throwable e) {
            throw new Error("Database snowflake server initialize error.", e);
        }

        this.heartbeatScheduler = new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = new Thread(r, "snowflake_worker_heartbeat");
            thread.setDaemon(true);
            thread.setPriority(Thread.MAX_PRIORITY);
            return thread;
        });
        heartbeatScheduler.scheduleWithFixedDelay(this::heartbeat, 5, 5, TimeUnit.MINUTES);
    }

    @Override
    public long generateId() {
        return snowflake.generateId();
    }

    @Override
    public void close() {
        Throwables.ThrowingSupplier.caught(() -> ThreadPoolExecutors.shutdown(heartbeatScheduler, 3));
        Throwables.ThrowingSupplier.caught(() -> jdbcTemplate.update(DEREGISTER_WORKER_SQL, bizTag, serverTag));
    }

    // -------------------------------------------------------private methods

    private int registryWorkerId(int workerIdBitLength) {
        int workerIdMaxCount = 1 << workerIdBitLength;
        List<SnowflakeWorker> registeredWorkers = jdbcTemplate.queryForStream(QUERY_ALL_SQL, ROW_MAPPER, bizTag).collect(Collectors.toList());

        if (registeredWorkers.size() > workerIdMaxCount / 2) {
            long oldestHeartbeatTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
            registeredWorkers
                .stream()
                .filter(e -> e.getHeartbeatTime() < oldestHeartbeatTime)
                .forEach(e -> jdbcTemplate.update(REMOVE_DEAD_SQL, e.getBizTag(), e.getServerTag(), e.getHeartbeatTime()));
            // re-query
            registeredWorkers = jdbcTemplate.queryForList(QUERY_ALL_SQL, SnowflakeWorker.class, bizTag);
        }

        SnowflakeWorker current = registeredWorkers.stream().filter(e -> e.equals(bizTag, serverTag)).findAny().orElse(null);
        if (current == null) {
            Set<Integer> usedWorkIds = registeredWorkers.stream().map(SnowflakeWorker::getWorkerId).collect(Collectors.toSet());
            List<Integer> usableWorkerIds = IntStream.range(0, workerIdMaxCount)
                .boxed()
                .filter(ObjectUtils.not(usedWorkIds::contains))
                .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(usableWorkerIds)) {
                throw new IllegalStateException("Not found usable worker id.");
            }

            Collections.shuffle(usableWorkerIds);
            for (Integer usableWorkerId : usableWorkerIds) {
                Object[] args = {bizTag, serverTag, usableWorkerId, System.currentTimeMillis()};
                try {
                    jdbcTemplate.update(REGISTER_WORKER_SQL, args);
                    LOG.info("Create snowflake worker success: {} | {} | {} | {}", args);
                    return usableWorkerId;
                } catch (Exception e) {
                    LOG.warn("Registry snowflake worker failed: " + e.getMessage() + ", args: {} | {} | {} | {}", args);
                }
            }
            throw new IllegalStateException("Cannot found usable worker id: " + bizTag + ", " + serverTag);
        } else {
            long currentTime = System.currentTimeMillis(), lastHeartbeatTime = current.getHeartbeatTime();
            if (currentTime < lastHeartbeatTime) {
                throw new ClockBackwardsException(String.format("Clock moved backwards: %s | %s | %d | %d", bizTag, serverTag, currentTime, lastHeartbeatTime));
            }
            Object[] args = {currentTime, bizTag, serverTag, lastHeartbeatTime};
            if (jdbcTemplate.update(REUSE_WORKER_SQL, args) == AFFECTED_ONE_ROW) {
                LOG.info("Reuse worker id success: {} | {} | {} | {}", args);
                return current.getWorkerId();
            } else {
                throw new IllegalStateException("Reuse worker id failed: " + bizTag + ", " + serverTag);
            }
        }
    }

    private void createTableIfNotExists() {
        if (hasTable()) {
            return;
        }

        try {
            jdbcTemplate.execute(CREATE_TABLE_SQL);
            LOG.info("Created table {} success.", TABLE_NAME);
        } catch (Throwable t) {
            if (hasTable()) {
                LOG.warn("Create table {} failed {}", TABLE_NAME, t.getMessage());
            } else {
                throw new Error("Create table " + TABLE_NAME + " error.", t);
            }
        }
    }

    private boolean hasTable() {
        Boolean result = jdbcTemplate.execute((ConnectionCallback<Boolean>) conn -> {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getTables(null, null, TABLE_NAME, null);
            return rs.next();
        });
        return Boolean.TRUE.equals(result);
    }

    private void heartbeat() {
        try {
            RetryTemplate.execute(() -> {
                long currentTimestamp = System.currentTimeMillis();
                Object[] args = {currentTimestamp, bizTag, serverTag};
                if (jdbcTemplate.update(HEARTBEAT_WORKER_SQL, args) == AFFECTED_ONE_ROW) {
                    LOG.info("Heartbeat worker id success: {} | {} | {}", args);
                } else {
                    LOG.error("Heartbeat worker id failed: {} | {} | {}", args);
                }
                return true;
            }, 5, 3000L);
        } catch (Throwable e) {
            LOG.error("Database snowflake server heartbeat error: " + bizTag + " | " + serverTag, e);
        }
    }

}

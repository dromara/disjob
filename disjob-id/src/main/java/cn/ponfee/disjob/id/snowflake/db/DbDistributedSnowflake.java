/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.id.snowflake.db;

import cn.ponfee.disjob.common.base.IdGenerator;
import cn.ponfee.disjob.common.base.RetryTemplate;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingSupplier;
import cn.ponfee.disjob.common.spring.JdbcTemplateWrapper;
import cn.ponfee.disjob.common.util.ObjectUtils;
import cn.ponfee.disjob.id.snowflake.ClockMovedBackwardsException;
import cn.ponfee.disjob.id.snowflake.Snowflake;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;

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
public class DbDistributedSnowflake implements IdGenerator, AutoCloseable {

    private final static Logger LOG = LoggerFactory.getLogger(DbDistributedSnowflake.class);

    private static final long EXPIRE_TIME_MILLIS = TimeUnit.HOURS.toMillis(12);

    private static final long HEARTBEAT_PERIOD_SECONDS = 60;

    private static final RowMapper<DbSnowflakeWorker> ROW_MAPPER = new BeanPropertyRowMapper<>(DbSnowflakeWorker.class);

    private static final int AFFECTED_ONE_ROW = 1;

    private static final String TABLE_NAME = "snowflake_worker";

    private static final String CREATE_TABLE_SQL =
        "CREATE TABLE IF NOT EXISTS `" + TABLE_NAME + "` (                                                                                              \n" +
        "  `id`              BIGINT        UNSIGNED  NOT NULL  AUTO_INCREMENT               COMMENT 'auto increment id',                                \n" +
        "  `biz_tag`         VARCHAR(60)             NOT NULL                               COMMENT 'biz tag',                                          \n" +
        "  `server_tag`      VARCHAR(128)            NOT NULL                               COMMENT 'server tag(ip:port)',                              \n" +
        "  `worker_id`       INT           UNSIGNED  NOT NULL                               COMMENT 'snowflake worker-id',                              \n" +
        "  `heartbeat_time`  BIGINT        UNSIGNED  NOT NULL                               COMMENT 'last heartbeat time',                              \n" +
        "  `updated_at`      DATETIME(3)             NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'last updated time' ON UPDATE CURRENT_TIMESTAMP(3), \n" +
        "  `created_at`      DATETIME(3)             NOT NULL  DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'created time',                                     \n" +
        "  PRIMARY KEY (`id`),                                                                                                                          \n" +
        "  UNIQUE KEY `uk_biztag_servertag` (`biz_tag`, `server_tag`),                                                                                  \n" +
        "  UNIQUE KEY `uk_biztag_workerid` (`biz_tag`, `worker_id`),                                                                                    \n" +
        "  KEY `ix_createdat` (`created_at`),                                                                                                           \n" +
        "  KEY `ix_updatedat` (`updated_at`)                                                                                                            \n" +
        ") ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Allocate snowflake worker-id';                           \n" ;

    private static final String QUERY_ALL_SQL = "SELECT biz_tag, server_tag, worker_id, heartbeat_time FROM " + TABLE_NAME + " WHERE biz_tag=?";

    private static final String REMOVE_DEAD_SQL = "DELETE FROM " + TABLE_NAME + " WHERE biz_tag=? AND heartbeat_time<?";

    private static final String REMOVE_INVALID_SQL = "DELETE FROM " + TABLE_NAME + " WHERE biz_tag=? AND server_tag=?";

    private static final String REGISTER_WORKER_SQL = "INSERT INTO " + TABLE_NAME + " (biz_tag, server_tag, worker_id, heartbeat_time) VALUES (?, ?, ?, ?)";

    private static final String DEREGISTER_WORKER_SQL = "DELETE FROM " + TABLE_NAME + " WHERE biz_tag=? AND server_tag=?";

    private static final String REUSE_WORKER_SQL = "UPDATE " + TABLE_NAME + " SET heartbeat_time=? WHERE biz_tag=? AND server_tag=? AND heartbeat_time=?";

    private static final String HEARTBEAT_WORKER_SQL = "UPDATE " + TABLE_NAME + " SET heartbeat_time=? WHERE biz_tag=? AND server_tag=?";

    private final JdbcTemplateWrapper jdbcTemplateWrapper;
    private final String bizTag;
    private final String serverTag;
    private final ScheduledExecutorService heartbeatScheduler;
    private final Snowflake snowflake;

    public DbDistributedSnowflake(JdbcTemplate jdbcTemplate, String bizTag, String serverTag) {
        this(jdbcTemplate, bizTag, serverTag, 14, 8);
    }

    public DbDistributedSnowflake(JdbcTemplate jdbcTemplate,
                                  String bizTag,
                                  String serverTag,
                                  int sequenceBitLength,
                                  int workerIdBitLength) {
        int len = sequenceBitLength + workerIdBitLength;
        Assert.isTrue(len <= 22, "Bit length(sequence + worker) cannot greater than 22, but actual=" + len);

        this.jdbcTemplateWrapper = JdbcTemplateWrapper.of(jdbcTemplate);
        this.bizTag = bizTag;
        this.serverTag = serverTag;

        try {
            RetryTemplate.execute(this::createTableIfNotExists, 5, 1000L);
        } catch (Throwable e) {
            throw new IllegalStateException("Create " + TABLE_NAME + " table failed.", e);
        }

        try {
            int workerId = RetryTemplate.execute(() -> registerWorkerId(workerIdBitLength), 5, 1000L);
            this.snowflake = new Snowflake(workerId, sequenceBitLength, workerIdBitLength);
        } catch (Throwable e) {
            throw new Error("Db snowflake server initialize error.", e);
        }

        this.heartbeatScheduler = new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = new Thread(r, "db_snowflake_worker_heartbeat");
            thread.setDaemon(true);
            thread.setPriority(Thread.MAX_PRIORITY);
            return thread;
        });
        heartbeatScheduler.scheduleWithFixedDelay(this::heartbeat, 1, HEARTBEAT_PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public long generateId() {
        return snowflake.generateId();
    }

    @Override
    public void close() {
        ThrowingSupplier.caught(() -> ThreadPoolExecutors.shutdown(heartbeatScheduler, 3));
        ThrowingSupplier.caught(() -> jdbcTemplateWrapper.delete(DEREGISTER_WORKER_SQL, bizTag, serverTag));
    }

    // -------------------------------------------------------private methods

    private int registerWorkerId(int workerIdBitLength) {
        int workerIdMaxCount = 1 << workerIdBitLength;
        List<DbSnowflakeWorker> registeredWorkers = jdbcTemplateWrapper.queryForList(QUERY_ALL_SQL, ROW_MAPPER, bizTag);
        DbSnowflakeWorker current = registeredWorkers.stream().filter(e -> e.equals(bizTag, serverTag)).findAny().orElse(null);

        if (current == null) {

            if (registeredWorkers.size() > workerIdMaxCount / 2) {
                long oldestTimeMillis = System.currentTimeMillis() - EXPIRE_TIME_MILLIS;
                jdbcTemplateWrapper.delete(REMOVE_DEAD_SQL, bizTag, oldestTimeMillis);

                // re-query
                registeredWorkers = jdbcTemplateWrapper.queryForList(QUERY_ALL_SQL, ROW_MAPPER, bizTag);
            }

            Set<Integer> usedWorkIds = registeredWorkers.stream().map(DbSnowflakeWorker::getWorkerId).collect(Collectors.toSet());
            List<Integer> usableWorkerIds = IntStream.range(0, workerIdMaxCount)
                .boxed()
                .filter(ObjectUtils.not(usedWorkIds::contains))
                .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(usableWorkerIds)) {
                throw new IllegalStateException("Not found usable db worker id.");
            }

            Collections.shuffle(usableWorkerIds);
            for (Integer usableWorkerId : usableWorkerIds) {
                Object[] args = {bizTag, serverTag, usableWorkerId, System.currentTimeMillis()};
                try {
                    jdbcTemplateWrapper.insert(REGISTER_WORKER_SQL, args);
                    LOG.info("Create snowflake db worker success: {} | {} | {} | {}", args);
                    return usableWorkerId;
                } catch (Exception e) {
                    LOG.warn("Registry snowflake db worker failed: " + e.getMessage() + ", args: {} | {} | {} | {}", args);
                }
            }
            throw new IllegalStateException("Cannot found usable db worker id: " + bizTag + ", " + serverTag);

        } else {

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
                throw new ClockMovedBackwardsException(String.format("Clock moved backwards: %s | %s | %d | %d", bizTag, serverTag, currentTime, lastHeartbeatTime));
            }
            Object[] args = {currentTime, bizTag, serverTag, lastHeartbeatTime};
            if (jdbcTemplateWrapper.update(REUSE_WORKER_SQL, args) == AFFECTED_ONE_ROW) {
                LOG.info("Reuse db worker id success: {} | {} | {} | {}", args);
                return workerId;
            }

            throw new IllegalStateException("Reuse db worker id failed: " + bizTag + ", " + serverTag);

        }
    }

    private void createTableIfNotExists() {
        if (hasTable()) {
            return;
        }

        try {
            jdbcTemplateWrapper.execute(CREATE_TABLE_SQL);
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
        Boolean result = jdbcTemplateWrapper.execute(conn -> {
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
                if (jdbcTemplateWrapper.update(HEARTBEAT_WORKER_SQL, args) == AFFECTED_ONE_ROW) {
                    LOG.info("Heartbeat db worker id success: {} | {} | {}", args);
                } else {
                    LOG.error("Heartbeat db worker id failed: {} | {} | {}", args);
                }
            }, 5, 3000L);
        } catch (Throwable e) {
            LOG.error("Db snowflake server heartbeat error: " + bizTag + " | " + serverTag, e);
        }
    }

}

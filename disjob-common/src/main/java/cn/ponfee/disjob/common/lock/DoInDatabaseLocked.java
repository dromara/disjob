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

package cn.ponfee.disjob.common.lock;

import cn.ponfee.disjob.common.base.RetryTemplate;
import cn.ponfee.disjob.common.concurrent.Threads;
import cn.ponfee.disjob.common.spring.JdbcTemplateWrapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.Assert;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Distributed lock based jdbc database.
 *
 * @author Ponfee
 */
public final class DoInDatabaseLocked implements DoInLocked {

    private static final String TABLE_NAME = "disjob_lock";

    private static final String CREATE_TABLE_DDL =
        "CREATE TABLE IF NOT EXISTS `" + TABLE_NAME + "` (                                       \n" +
        "  `id`    BIGINT       UNSIGNED  NOT NULL  AUTO_INCREMENT  COMMENT 'auto increment id', \n" +
        "  `name`  VARCHAR(60)            NOT NULL                  COMMENT 'lock name',         \n" +
         "  PRIMARY KEY (`id`),                                                                  \n" +
        "  UNIQUE KEY `uk_name` (`name`)                                                         \n" +
        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='database lock';    \n" ;

    private static final String INSERT_SQL = "INSERT INTO " + TABLE_NAME + " (name) VALUES (?)";

    private static final String GET_SQL    = "SELECT 1 FROM " + TABLE_NAME + " WHERE name=?";

    private static final String LOCK_SQL   = GET_SQL + " FOR UPDATE";

    /**
     * Spring jdbc template wrapper.
     */
    private final JdbcTemplateWrapper jdbcTemplateWrapper;

    /**
     * Lock name
     */
    private final String lockName;

    public DoInDatabaseLocked(JdbcTemplate jdbcTemplate, String lockName) {
        this.jdbcTemplateWrapper = JdbcTemplateWrapper.of(jdbcTemplate);
        this.lockName = lockName;

        // create table
        jdbcTemplateWrapper.createTableIfNotExists(TABLE_NAME, CREATE_TABLE_DDL);

        // initialize lock
        try {
            RetryTemplate.execute(this::initializeLockIfNecessary, 3, 1000L);
        } catch (Throwable e) {
            Threads.interruptIfNecessary(e);
            throw new Error("Initialize lock '" + lockName + "' failed.", e);
        }
    }

    @Override
    public <T> T action(Callable<T> caller) {
        return jdbcTemplateWrapper.executeInTransaction(psCreator -> {
            PreparedStatement preparedStatement = psCreator.apply(LOCK_SQL);
            preparedStatement.setString(1, lockName);
            ResultSet rs = preparedStatement.executeQuery();
            Assert.state(rs.next() && rs.getInt(1) == 1, () -> "Lock Not found '" + lockName + "'.");
            // 关闭一个Statement对象同时也会使得该对象创建的所有ResultSet对象被关闭，即：可以不显示关闭ResultSet
            // ResultSet所持有的资源不会立刻被释放，直到GC执行，因此明确地关闭ResultSet是一个更好的做法
            JdbcUtils.closeResultSet(rs);
            return caller.call();
        });
    }

    private void initializeLockIfNecessary() {
        if (getLockId() != null) {
            return;
        }
        jdbcTemplateWrapper.insert(INSERT_SQL, lockName);
        Objects.requireNonNull(getLockId());
    }

    private Integer getLockId() {
        return jdbcTemplateWrapper.get(GET_SQL, JdbcTemplateWrapper.INTEGER_ROW_MAPPER, lockName);
    }

}

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

package cn.ponfee.disjob.common.spring;

import cn.ponfee.disjob.common.base.RetryTemplate;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingConsumer;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingFunction;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Wrapped spring jdbc template.
 *
 * @author Ponfee
 */
public final class JdbcTemplateWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcTemplateWrapper.class);

    public static final RowMapper<String> STRING_ROW_MAPPER = new SingleColumnRowMapper<>(String.class);
    public static final RowMapper<Long> LONG_ROW_MAPPER = new SingleColumnRowMapper<>(Long.class);
    public static final RowMapper<Integer> INTEGER_ROW_MAPPER = new SingleColumnRowMapper<>(Integer.class);
    private static final ConcurrentMap<String, Boolean> EXISTS_TABLE = new ConcurrentHashMap<>();

    private final JdbcTemplate jdbcTemplate;

    private JdbcTemplateWrapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public static JdbcTemplateWrapper of(JdbcTemplate jdbcTemplate) {
        return new JdbcTemplateWrapper(jdbcTemplate);
    }

    public JdbcTemplate jdbcTemplate() {
        return jdbcTemplate;
    }

    public int insert(String sql, Object... args) {
        Assert.isTrue(sql.startsWith("INSERT "), () -> "Invalid INSERT sql: " + sql);
        return jdbcTemplate.update(sql, args);
    }

    public int update(String sql, Object... args) {
        Assert.isTrue(sql.startsWith("UPDATE "), () -> "Invalid UPDATE sql: " + sql);
        return jdbcTemplate.update(sql, args);
    }

    public int delete(String sql, Object... args) {
        Assert.isTrue(sql.startsWith("DELETE "), () -> "Invalid DELETE sql: " + sql);
        return jdbcTemplate.update(sql, args);
    }

    public <T> List<T> list(String sql, RowMapper<T> rowMapper, Object... args) {
        Assert.isTrue(sql.startsWith("SELECT "), () -> "Invalid LIST sql: " + sql);
        return jdbcTemplate.query(sql, rowMapper, args);
    }

    public <T> T get(String sql, RowMapper<T> rowMapper, Object... args) {
        Assert.isTrue(sql.startsWith("SELECT "), () -> "Invalid GET sql: " + sql);
        List<T> result = jdbcTemplate.query(sql, rowMapper, args);
        if (CollectionUtils.isEmpty(result)) {
            return null;
        } else if (result.size() == 1) {
            return result.get(0);
        } else {
            throw new IncorrectResultSizeDataAccessException(1, result.size());
        }
    }

    public void executeInTransaction(ThrowingConsumer<ThrowingFunction<String, PreparedStatement, ?>, ?> action) {
        executeInTransaction(action.toFunction(null));
    }

    public <T> T executeInTransaction(ThrowingFunction<ThrowingFunction<String, PreparedStatement, ?>, T, ?> action) {
        return jdbcTemplate.execute((ConnectionCallback<T>) con -> {
            PreparedStatementCreator psCreator = null;
            boolean originalAutoCommit = con.getAutoCommit();
            try {
                if (originalAutoCommit) {
                    con.setAutoCommit(false);
                }
                psCreator = new PreparedStatementCreator(con);
                T result = action.apply(psCreator);
                con.commit();
                return result;
            } catch (Throwable t) {
                ThrowingRunnable.doCaught(con::rollback, "Connection rollback occur error: {}");
                return ExceptionUtils.rethrow(t);
            } finally {
                if (originalAutoCommit) {
                    // isClosed: connection is proxy by CloseSuppressingInvocationHandler, always false
                    ThrowingRunnable.doCaught(() -> con.setAutoCommit(true), "Restore auto-commit occur error: {}");
                }
                if (psCreator != null) {
                    psCreator.close();
                }
            }
        });
    }

    public void createTableIfNotExists(String tableName, String createTableDdl) {
        EXISTS_TABLE.computeIfAbsent(tableName.trim().toLowerCase(), key -> {
            try {
                return RetryTemplate.execute(() -> {
                    if (existsTable(key)) {
                        return true;
                    }
                    jdbcTemplate.execute(createTableDdl);
                    Assert.state(existsTable(key), () -> "Create table " + key + " failed.");
                    LOG.info("Created table {} success.", key);
                    return true;
                }, 3, 1000L);
            } catch (Throwable e) {
                return ExceptionUtils.rethrow(e);
            }
        });
    }

    public boolean existsTable(String tableName) {
        Boolean result = jdbcTemplate.execute((ConnectionCallback<Boolean>) conn -> {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getTables(null, null, tableName, new String[]{"TABLE"});
            boolean exists = rs.next() && tableName.equalsIgnoreCase(rs.getString(3));
            JdbcUtils.closeResultSet(rs);
            return exists;
        });
        return Boolean.TRUE.equals(result);
    }

    public String getServerInfo() {
        try {
            return jdbcTemplate.execute((ConnectionCallback<String>) conn -> {
                DatabaseMetaData meta = conn.getMetaData();
                String productName = meta.getDatabaseProductName();
                String productVersion = meta.getDatabaseProductVersion();
                String jdbcUrl = meta.getURL();
                return String.format("Product=%s(%s), Url=%s", productName, productVersion, jdbcUrl);
            });
        } catch (Exception e) {
            return "Get database server info failed: " + e.getMessage();
        }
    }

    // -----------------------------------------------------------------private methods

    private static class PreparedStatementCreator implements ThrowingFunction<String, PreparedStatement, Throwable> {
        private final Connection con;
        private final List<PreparedStatement> psList = new LinkedList<>();

        private PreparedStatementCreator(Connection con) {
            this.con = con;
        }

        @Override
        public PreparedStatement apply(String sql) throws Throwable {
            PreparedStatement ps = con.prepareStatement(sql);
            psList.add(ps);
            return ps;
        }

        public void close() {
            Lists.reverse(psList).forEach(JdbcUtils::closeStatement);
        }
    }

}

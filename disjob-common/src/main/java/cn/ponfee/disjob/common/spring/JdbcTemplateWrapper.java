/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.spring;

import cn.ponfee.disjob.common.base.Releasable;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingFunction;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Wrapped spring jdbc template.
 *
 * @author Ponfee
 */
public final class JdbcTemplateWrapper implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcTemplateWrapper.class);

    public static final int AFFECTED_ONE_ROW = 1;

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
        Assert.isTrue(sql.startsWith("INSERT "), () -> "Invalid DELETE sql: " + sql);
        return jdbcTemplate.update(sql, args);
    }

    public int update(String sql, Object... args) {
        Assert.isTrue(sql.startsWith("UPDATE "), () -> "Invalid DELETE sql: " + sql);
        return jdbcTemplate.update(sql, args);
    }

    public int delete(String sql, Object... args) {
        Assert.isTrue(sql.startsWith("DELETE "), () -> "Invalid DELETE sql: " + sql);
        return jdbcTemplate.update(sql, args);
    }

    public <T> List<T> queryForList(RowMapper<T> rowMapper, String sql, Object... args) {
        Assert.isTrue(sql.startsWith("SELECT "), () -> "Invalid SELECT sql: " + sql);
        return jdbcTemplate.queryForStream(sql, rowMapper, args).collect(Collectors.toList());
    }

    public <T> T executeInTransaction(ThrowingFunction<ThrowingFunction<String, PreparedStatement, ?>, T, ?> action) {
        return jdbcTemplate.execute((ConnectionCallback<T>) con -> {
            Boolean previousAutoCommit = null;
            final List<PreparedStatement> preparedStatements = new ArrayList<>();
            ThrowingFunction<String, PreparedStatement, ?> function = sql -> {
                PreparedStatement preparedStatement = con.prepareStatement(sql);
                preparedStatements.add(preparedStatement);
                return preparedStatement;
            };
            try {
                previousAutoCommit = con.getAutoCommit();
                con.setAutoCommit(false);
                T result = action.apply(function);
                con.commit();
                return result;
            } catch (Throwable t) {
                con.rollback();
                return ExceptionUtils.rethrow(t);
            } finally {
                if (previousAutoCommit != null) {
                    try {
                        // restore the auto-commit config
                        con.setAutoCommit(previousAutoCommit);
                    } catch (Throwable t) {
                        LOG.error("Restore connection auto-commit occur error.", t);
                    }
                }
                for (PreparedStatement each : Lists.reverse(preparedStatements)) {
                    try {
                        each.close();
                    } catch (Throwable t) {
                        LOG.error("Close prepare statement occur error.", t);
                    }
                }
            }
        });
    }

    public void createTableIfNotExists(String tableName, String createTableDdl) {
        if (existsTable(tableName)) {
            return;
        }

        try {
            jdbcTemplate.execute(createTableDdl);
            LOG.info("Created table {} success.", tableName);
        } catch (Throwable t) {
            if (existsTable(tableName)) {
                LOG.warn("Create table {} failed {}", tableName, t.getMessage());
            } else {
                throw new Error("Create table " + tableName + " error.", t);
            }
        }
    }

    public boolean existsTable(String tableName) {
        Boolean result = jdbcTemplate.execute((ConnectionCallback<Boolean>) conn -> {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getTables(null, null, tableName, null);
            return rs.next();
        });
        return Boolean.TRUE.equals(result);
    }

    @Override
    public void close() throws IOException {
        DataSource dataSource = jdbcTemplate.getDataSource();
        if (dataSource != null) {
            ThrowingRunnable.execute(() -> Releasable.release(dataSource));
        }
    }

}

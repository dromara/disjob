/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.spring;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Wrapped jdbc template.
 *
 * @author Ponfee
 */
public class JdbcTemplateWrapper {

    private final JdbcTemplate jdbcTemplate;

    private JdbcTemplateWrapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public static JdbcTemplateWrapper of(JdbcTemplate jdbcTemplate) {
        return new JdbcTemplateWrapper(jdbcTemplate);
    }

    public void execute(String sql) {
        jdbcTemplate.execute(sql);
    }

    public <T> T execute(ConnectionCallback<T> action) {
        return jdbcTemplate.execute(action);
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

}

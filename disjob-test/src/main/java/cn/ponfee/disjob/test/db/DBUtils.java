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

package cn.ponfee.disjob.test.db;

import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.common.util.Numbers;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 内存DB测试工具类
 *
 * @author Ponfee
 */
public class DBUtils {

    private static final List<String> SCRIPTS_CLASSPATH = Arrays.asList("mysql-disjob.sql", "mysql-disjob_admin.sql");

    public static final String DB_NAME = "disjob";
    public static final String USERNAME = "disjob";
    public static final String PASSWORD = "disjob$123456";

    public static JdbcTemplate createJdbcTemplate(String url, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        return new JdbcTemplate(new HikariDataSource(config));
    }

    public static List<String> loadScript() {
        return SCRIPTS_CLASSPATH.stream().map(DBUtils::loadScript).collect(Collectors.toList());
    }

    public static String loadScript(String scriptClasspath) {
        try {
            return IOUtils.resourceToString(scriptClasspath, StandardCharsets.UTF_8, DBUtils.class.getClassLoader());
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    public static void testNativeConnection(String driver, String url, String username, String password) throws Exception {
        // 非必须：DriverManager.getConnection(url, user, password)时会根据url自动识别来加载Driver实现类
        // Class.forName(driver);
        Connection conn = DriverManager.getConnection(url, username, password);

        System.out.println("Testing Database, URL=" + url);
        Statement ddlStatement = conn.createStatement();
        ddlStatement.execute("DROP TABLE IF EXISTS test");
        ddlStatement.execute("CREATE TABLE IF NOT EXISTS test(id INT, `name` VARCHAR(30))");

        // insert
        List<String> data = new ArrayList<>();
        PreparedStatement insertStatement = conn.prepareStatement("INSERT INTO test VALUES(?, ?)");
        for (int i = 0; i < 10; i++) {
            String str = RandomStringUtils.randomAlphanumeric(4);
            data.add(str);
            insertStatement.setInt(1, i);
            insertStatement.setString(2, str);
            // autocommit is on by default, so this commits as well
            insertStatement.execute();
        }
        JdbcUtils.closeStatement(insertStatement);

        // query
        List<String> result = new ArrayList<>();
        PreparedStatement queryStatement = conn.prepareStatement("SELECT `name` FROM test ORDER BY id ASC");
        ResultSet resultSet = queryStatement.executeQuery();
        while (resultSet.next()) {
            result.add(resultSet.getString(1));
        }
        JdbcUtils.closeResultSet(resultSet);
        JdbcUtils.closeStatement(queryStatement);

        Assert.isTrue(CollectionUtils.isEqualCollection(data, result), () -> Jsons.toJson(data) + " != " + Jsons.toJson(result));
        ddlStatement.execute("DROP TABLE IF EXISTS test");
        JdbcUtils.closeStatement(ddlStatement);

        conn.close();
    }

    public static void testJdbcTemplate(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("DROP TABLE IF EXISTS test");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS test(id INT, `name` VARCHAR(30))");

        List<String> data = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            String str = RandomStringUtils.randomAlphanumeric(4);
            data.add(str);
            jdbcTemplate.update("INSERT INTO test VALUES(?, ?)", i, str);
        }

        List<String> result = jdbcTemplate.queryForList("SELECT `name` FROM test ORDER BY id ASC", String.class);
        Assert.isTrue(
            CollectionUtils.isEqualCollection(data, result),
            () -> Jsons.toJson(data) + " != " + Jsons.toJson(result)
        );

        jdbcTemplate.execute("DROP TABLE IF EXISTS test");
    }

    public static void testMysqlVersion(JdbcTemplate jdbcTemplate) {
        String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
        System.out.println("Version: " + version);

        int expect = 1;
        Integer actual = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        Assert.isTrue(expect == actual, () -> expect + " != " + actual);
    }

    public static void testQuerySchedJob(JdbcTemplate jdbcTemplate) {
        List<Map<String, Object>> result = jdbcTemplate.queryForList("SELECT * FROM sched_job ORDER BY id ASC");
        long expect = 1003164910267351000L;
        long actual = Numbers.toLong(result.get(0).get("job_id"));
        Assert.isTrue(expect == actual, () -> expect + " != " + actual);
        System.out.println("Query result: " + Jsons.toJson(result));
    }

}

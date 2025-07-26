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

package cn.ponfee.disjob.test.util;

import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfiguration;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.util.List;

/**
 * MariaDB test
 *
 * @author Ponfee
 */
@Disabled
public class MariaDBTest {

    private static final String HOST_IP = "192.168.1.102";
    private static final String DEV_USER = "dev_user";
    private static final String DB_NAME = "db_name";

    private final QueryRunner qr = new QueryRunner();
    private volatile DB db;
    private volatile Integer port;

    @BeforeEach
    public void setUp() throws Exception {
        DBConfiguration config = DBConfigurationBuilder.newBuilder()
            // detect free port
            .setPort(0)
            .setSecurityDisabled(false)
            .build();
        DB db = DB.newEmbeddedDB(config);
        db.start();
        db.createDB(DB_NAME, "root", "");
        this.db = db;
        this.port = config.getPort();
    }

    @AfterEach
    public void tearDown() {
        if (this.db != null) {
            ThrowingRunnable.doCaught(this.db::stop);
        }
        this.db = null;
        this.port = null;
    }

    @Test
    public void test1() throws Exception {
        testQuery("localhost", "root", "", DB_NAME);
        testQuery("127.0.0.1", "root", "", DB_NAME);

        Assertions.assertThat(qr.query(createConnection("localhost", "root", ""), "SELECT CURRENT_USER()", new ColumnListHandler<>()).toString())
            .isEqualTo("[root@localhost]");
        Assertions.assertThat(qr.query(createConnection("127.0.0.1", "root", ""), "SELECT CURRENT_USER()", new ColumnListHandler<>()).toString())
            .isEqualTo("[root@localhost]");
        Assertions.assertThat(qr.query(createConnection("localhost", DEV_USER, ""), "SELECT CURRENT_USER()", new ColumnListHandler<>()).toString())
            .isEqualTo("[@localhost]");
        Assertions.assertThat(qr.query(createConnection("127.0.0.1", DEV_USER, ""), "SELECT CURRENT_USER()", new ColumnListHandler<>()).toString())
            .isEqualTo("[@localhost]");

        Assertions.assertThatThrownBy(() -> createConnection("127.0.0.1", DEV_USER, "", DB_NAME))
            .isInstanceOf(SQLSyntaxErrorException.class)
            .hasMessage("Access denied for user ''@'localhost' to database 'db_name'");

        Assertions.assertThatThrownBy(() -> createConnection(HOST_IP, "root", ""))
            .isInstanceOf(SQLException.class)
            .hasMessage("null,  message from server: \"Host '192.168.1.102' is not allowed to connect to this MariaDB server\"");

        Assertions.assertThatThrownBy(() -> createConnection(HOST_IP, DEV_USER, ""))
            .isInstanceOf(SQLException.class)
            .hasMessage("null,  message from server: \"Host '192.168.1.102' is not allowed to connect to this MariaDB server\"");
    }

    @Test
    public void test2() throws Exception {
        // root身份从localhost主机登录
        Connection conn = createConnection("localhost", "root", "");

        // 更改root localhost主机密码
        qr.update(conn, "ALTER USER 'root'@'localhost' IDENTIFIED BY 'root_localhost_password'");
        DbUtils.close(conn);

        // test
        Assertions.assertThatThrownBy(() -> createConnection("localhost", "root", ""))
            .isInstanceOf(SQLException.class)
            .hasMessage("Access denied for user 'root'@'localhost' (using password: NO)");

        Assertions.assertThatThrownBy(() -> createConnection("127.0.0.1", "root", ""))
            .isInstanceOf(SQLException.class)
            .hasMessage("Access denied for user 'root'@'localhost' (using password: NO)");

        Assertions.assertThatThrownBy(() -> createConnection("localhost", DEV_USER, "root_localhost_password"))
            .isInstanceOf(SQLException.class)
            .hasMessage("Access denied for user 'dev_user'@'localhost' (using password: YES)");

        Assertions.assertThatThrownBy(() -> createConnection("127.0.0.1", DEV_USER, "root_localhost_password"))
            .isInstanceOf(SQLException.class)
            .hasMessage("Access denied for user 'dev_user'@'localhost' (using password: YES)");

        testQuery("localhost", "root", "root_localhost_password", DB_NAME);
        testQuery("127.0.0.1", "root", "root_localhost_password", DB_NAME);
    }

    @Test
    public void test3() throws Exception {
        // root身份从localhost主机登录
        Connection conn1 = createConnection("localhost", "root", "");

        // 更改root %主机密码
        qr.update(conn1, "CREATE USER 'root'@'%' IDENTIFIED BY 'root_%_password'");
        qr.update(conn1, "GRANT ALL PRIVILEGES ON *.* TO 'root'@'%'");
        DbUtils.close(conn1);

        // test
        testQuery("localhost", "root", "", DB_NAME);
        testQuery("127.0.0.1", "root", "", DB_NAME);
        createConnection("localhost", DEV_USER, "");
        createConnection("127.0.0.1", DEV_USER, "");

        Assertions.assertThatThrownBy(() -> createConnection("localhost", "root", "root_%_password"))
            .isInstanceOf(SQLException.class)
            .hasMessage("Access denied for user 'root'@'localhost' (using password: YES)");

        Assertions.assertThatThrownBy(() -> createConnection("127.0.0.1", "root", "root_%_password"))
            .isInstanceOf(SQLException.class)
            .hasMessage("Access denied for user 'root'@'localhost' (using password: YES)");

        Assertions.assertThatThrownBy(() -> createConnection("localhost", DEV_USER, "root_%_password"))
            .isInstanceOf(SQLException.class)
            .hasMessage("Access denied for user 'dev_user'@'localhost' (using password: YES)");

        Assertions.assertThatThrownBy(() -> createConnection("127.0.0.1", DEV_USER, "root_%_password"))
            .isInstanceOf(SQLException.class)
            .hasMessage("Access denied for user 'dev_user'@'localhost' (using password: YES)");

        testQuery(HOST_IP, "root", "root_%_password", DB_NAME);


        // root身份从%主机登录
        Connection conn2 = createConnection(HOST_IP, "root", "root_%_password");
        // 创建dev_user用户并更改localhost密码
        qr.update(conn2, "CREATE USER 'dev_user'@'localhost' IDENTIFIED BY 'dev_user_localhost_password'");
        Assertions.assertThatThrownBy(() -> qr.update(conn2, "GRANT ALL PRIVILEGES ON db_name.* TO 'dev_user'@'localhost'"))
            .isInstanceOf(SQLException.class)
            .hasMessage("Access denied for user 'root'@'%' to database 'db_name' Query: GRANT ALL PRIVILEGES ON db_name.* TO 'dev_user'@'localhost' Parameters: []");
    }

    @Test
    public void test4() throws Exception {
        // root身份从localhost主机登录
        Connection conn1 = createConnection("localhost", "root", "");

        // 更改root %主机密码
        qr.update(conn1, "CREATE USER 'root'@'%' IDENTIFIED BY 'root_%_password'");
        qr.update(conn1, "GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION");
        DbUtils.close(conn1);

        testQuery(HOST_IP, "root", "root_%_password", DB_NAME);

        // root身份从%主机登录
        Connection conn2 = createConnection(HOST_IP, "root", "root_%_password");
        // 创建dev_user用户并更改localhost密码
        qr.update(conn2, "CREATE USER 'dev_user'@'localhost' IDENTIFIED BY 'dev_user_localhost_password'");
        qr.update(conn2, "GRANT ALL PRIVILEGES ON db_name.* TO 'dev_user'@'localhost'");
        DbUtils.close(conn2);

        // test
        Assertions.assertThatThrownBy(() -> createConnection("localhost", DEV_USER, ""))
            .isInstanceOf(SQLException.class)
            .hasMessage("Access denied for user 'dev_user'@'localhost' (using password: NO)");

        Assertions.assertThatThrownBy(() -> createConnection("127.0.0.1", DEV_USER, ""))
            .isInstanceOf(SQLException.class)
            .hasMessage("Access denied for user 'dev_user'@'localhost' (using password: NO)");

        testQuery("localhost", DEV_USER, "dev_user_localhost_password", DB_NAME);
        testQuery("127.0.0.1", DEV_USER, "dev_user_localhost_password", DB_NAME);

        Assertions.assertThatThrownBy(() -> createConnection(HOST_IP, DEV_USER, "dev_user_localhost_password"))
            .isInstanceOf(SQLException.class)
            .hasMessage("Access denied for user 'dev_user'@'192.168.1.102' (using password: YES)");
    }

    @Test
    public void test5() throws Exception {
        // root身份从localhost主机登录
        Connection conn1 = createConnection("localhost", "root", "");
        testQuery("localhost", "root", "", DB_NAME);
        testQuery("127.0.0.1", "root", "", DB_NAME);

        // 更改root %主机密码
        qr.update(conn1, "CREATE USER 'root'@'%' IDENTIFIED BY 'root_%_password'");
        qr.update(conn1, "GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION");
        DbUtils.close(conn1);
        testQuery(HOST_IP, "root", "root_%_password", DB_NAME);

        // root身份从%主机登录
        Connection conn2 = createConnection(HOST_IP, "root", "root_%_password");
        // 创建dev_user用户并更改localhost密码
        qr.update(conn2, "CREATE USER 'dev_user'@'localhost' IDENTIFIED BY 'dev_user_password'");
        qr.update(conn2, "CREATE USER 'dev_user'@'%' IDENTIFIED BY 'dev_user_password'");
        qr.update(conn2, "GRANT ALL PRIVILEGES ON db_name.* TO 'dev_user'@'localhost'");
        qr.update(conn2, "GRANT ALL PRIVILEGES ON db_name.* TO 'dev_user'@'%'");
        DbUtils.close(conn2);
        testQuery("localhost", DEV_USER, "dev_user_password", DB_NAME);
        testQuery("127.0.0.1", DEV_USER, "dev_user_password", DB_NAME);
        testQuery(HOST_IP, DEV_USER, "dev_user_password", DB_NAME);
    }

    @Test
    public void test6() throws Exception {
        // root身份从localhost主机登录
        Connection conn = createConnection("localhost", "root", "");
        testQuery("localhost", "root", "", DB_NAME);
        testQuery("127.0.0.1", "root", "", DB_NAME);

        // 创建dev_user用户并更改localhost密码
        qr.update(conn, "CREATE USER 'dev_user'@'localhost' IDENTIFIED BY 'dev_user_password'");
        qr.update(conn, "CREATE USER 'dev_user'@'%' IDENTIFIED BY 'dev_user_password'");
        qr.update(conn, "GRANT ALL PRIVILEGES ON db_name.* TO 'dev_user'@'localhost'");
        qr.update(conn, "GRANT ALL PRIVILEGES ON db_name.* TO 'dev_user'@'%'");
        DbUtils.close(conn);
        testQuery("localhost", DEV_USER, "dev_user_password", DB_NAME);
        testQuery("127.0.0.1", DEV_USER, "dev_user_password", DB_NAME);
        testQuery(HOST_IP, DEV_USER, "dev_user_password", DB_NAME);

        testQuery("localhost", "root", "", DB_NAME);
        testQuery("127.0.0.1", "root", "", DB_NAME);
    }

    // --------------------------------------------------------

    private Connection createConnection(String host, String user, String password) throws Exception {
        return createConnection(host, user, password, null);
    }

    private Connection createConnection(String host, String user, String password, String dbName) throws Exception {
        return DriverManager.getConnection(String.format("jdbc:mysql://%s:%d", host, port) + (dbName == null ? "" : "/" + dbName), user, password);
    }

    private void testQuery(String host, String user, String password, String dbName) throws Exception {
        Connection conn1 = createConnection(host, user, password);
        Assertions.assertThatThrownBy(() -> qr.update(conn1, "DROP TABLE IF EXISTS hello"))
            .isInstanceOf(SQLException.class)
            .hasMessage("No database selected Query: DROP TABLE IF EXISTS hello Parameters: []");
        DbUtils.close(conn1);

        Connection conn2 = createConnection(host, user, password, dbName);
        qr.update(conn2, "DROP TABLE IF EXISTS hello");
        qr.update(conn2, "CREATE TABLE hello(world VARCHAR(100))");
        qr.update(conn2, "INSERT INTO hello VALUES ('Hello world!')");
        List<String> results = qr.query(conn2, "SELECT * FROM hello", new ColumnListHandler<>());
        Assertions.assertThat(results.size()).isEqualTo(1);
        Assertions.assertThat(results.get(0)).isEqualTo("Hello world!");
        DbUtils.close(conn2);
    }

}

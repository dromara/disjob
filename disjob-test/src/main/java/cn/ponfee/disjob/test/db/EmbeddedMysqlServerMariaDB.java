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

import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfiguration;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import cn.ponfee.disjob.common.concurrent.ShutdownHookManager;
import cn.ponfee.disjob.common.util.Files;
import cn.ponfee.disjob.common.util.MavenProjects;
import org.apache.commons.io.IOUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static cn.ponfee.disjob.test.db.DBUtils.*;

/**
 * <pre>
 * MariaDB Server
 * SELECT VERSION()  ->  10.2.11-MariaDB
 * MacOS如果启动失败报"未找到openssl"错误，参考安装步骤：“/disjob-test/DB/MariaDB/README.md”
 *
 * username: root
 * password: 无需密码
 *
 *
 * mysql:
 *  Maven GAV    -> com.mysql:mysql-connector-j:8.0.33
 *  jdbc url     -> jdbc:mysql://localhost:3306/disjob
 *  driver class -> com.mysql.cj.jdbc.Driver
 *
 * MariaDB:
 *  Maven GAV    -> org.mariadb.jdbc:mariadb-java-client:3.1.4
 *  jdbc url     -> jdbc:mariadb://localhost:3306/disjob
 *  driver class -> org.mariadb.jdbc.Driver
 * </pre>
 *
 * @author Ponfee
 */
public class EmbeddedMysqlServerMariaDB {

    public static void main(String[] args) throws Exception {
        start(3306);
    }

    public static void start(int port) throws Exception {
        DBConfiguration configuration = DBConfigurationBuilder.newBuilder()
            .setPort(port) // OR, default: setPort(0); => autom. detect free port
            .setSecurityDisabled(false) // 是否“--skip-grant-tables”，默认为true(即启用跳过权限验证)，这里设置为false
            //.addArg("--user=xxx") // 以操作系统的xxx用户身份启动mysqld服务(慎用root用户)：base/bin/mysqld --user=xxx
            .setBaseDir(createDirectory("base"))
            .setDataDir(createDirectory("data"))
            .addArg("--character-set-server=utf8mb4")
            .addArg("--collation-server=utf8mb4_bin")
            .addArg("--default-time-zone=+08:00")
            .build();

        // 安装mariadb: base/bin/mysql_install_db
        DB db = DB.newEmbeddedDB(configuration);
        System.out.println("Embedded maria db starting...");
        ShutdownHookManager.addShutdownHook(Integer.MAX_VALUE, db::stop);
        db.start();
        db.run("ALTER USER 'root'@'localhost' IDENTIFIED BY ''", configuration.isWindows() ? "root" : System.getProperty("user.name"), "");
        for (String script : DBUtils.loadScript()) {
            // 以数据库的root用户身份连接mysql：base/bin/mysql -h xxx -u root -p
            // script = correctScriptForMariaDB(script);
            db.source(IOUtils.toInputStream(script, StandardCharsets.UTF_8), "root", "", null);
        }

        String jdbcUrl = "jdbc:mysql://localhost:" + port + "/" + DB_NAME;
        JdbcTemplate jdbcTemplate = DBUtils.createJdbcTemplate(jdbcUrl, USERNAME, PASSWORD);

        System.out.println("\n--------------------------------------------------------testDatabase");
        DBUtils.testNativeConnection("com.mysql.cj.jdbc.Driver", jdbcUrl, USERNAME, PASSWORD);

        System.out.println("\n--------------------------------------------------------testMysql");
        DBUtils.testMysqlVersion(jdbcTemplate);

        System.out.println("\n--------------------------------------------------------testJdbcTemplate");
        DBUtils.testJdbcTemplate(jdbcTemplate);

        System.out.println("\n--------------------------------------------------------testQuerySql");
        DBUtils.testQuerySchedJob(jdbcTemplate);

        System.out.println("Embedded maria db started!");
    }

    private static File createDirectory(String name) throws IOException {
        String dataDir = MavenProjects.getProjectBaseDir() + "/target/mariadb/" + name;
        Files.cleanOrMakeDir(dataDir);
        return new File(dataDir);
    }

    /*
    private static String correctScriptForMariaDB(String script) {
        return Arrays.stream(script.split("\n"))
            // fix error: The MariaDB server is running with the --skip-grant-tables option so it cannot execute this statement
            .filter(s -> !StringUtils.startsWithAny(s, "DROP USER ", "CREATE USER ", "GRANT ALL PRIVILEGES ON ", "FLUSH PRIVILEGES;"))
            .collect(Collectors.joining("\n"));
    }
    */

}

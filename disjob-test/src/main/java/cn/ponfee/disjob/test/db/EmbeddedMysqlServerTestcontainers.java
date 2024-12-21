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

import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static cn.ponfee.disjob.test.db.DBUtils.*;

/**
 * <pre>
 * Docker mysql for testcontainers
 *
 * docker pull mysql:8.4.3
 *
 * SELECT VERSION()  ->  8.4.3
 *
 * dependency maven junit:junit
 *
 * username: root
 * password: 无需密码
 * </pre>
 *
 * <a href="https://hub.docker.com/_/mysql/tags">docker官网查看版本</a>
 *
 * @author Ponfee
 */
public class EmbeddedMysqlServerTestcontainers {
    private static final List<String> PORT_BINDINGS = Collections.singletonList("3306:3306");

    public static void main(String[] args) throws Exception {
        DockerImageName dockerImage = DockerImageName.parse("mysql:8.4.3").asCompatibleSubstituteFor("mysql");
        try (MySQLContainer<?> mySqlContainer = new MySQLContainer<>(dockerImage)
            //.withConfigurationOverride("mysql_conf_override") // resource file: “resources/mysql_conf_override/my.cnf”
            .withPrivilegedMode(true)
            .withUsername("root")
            .withPassword("")
            .withDatabaseName("test")
            .withEnv("MYSQL_ROOT_HOST", "%")
            //.withInitScript(DISJOB_SCRIPT_CLASSPATH)
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(EmbeddedMysqlServerTestcontainers.class)))
        ) {
            mySqlContainer.setPortBindings(PORT_BINDINGS);
            //mySqlContainer.execInContainer("mysqld --skip-grant-tables");

            System.out.println("Embedded docker mysql starting...");
            mySqlContainer.start();


            Assert.isTrue(mySqlContainer.isCreated(), "Created error.");
            Assert.isTrue(mySqlContainer.isRunning(), "Running error.");

            // scriptPath只用于打印日志，此处直接设置为空字符串
            String scriptPath = "";
            String jdbcUrlParameter = "?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=2000&socketTimeout=5000";
            ScriptUtils.executeDatabaseScript(new JdbcDatabaseDelegate(mySqlContainer, jdbcUrlParameter), scriptPath, DBUtils.loadScript(DISJOB_ADMIN_SCRIPT_CLASSPATH));
            ScriptUtils.executeDatabaseScript(new JdbcDatabaseDelegate(mySqlContainer, jdbcUrlParameter), scriptPath, DBUtils.loadScript(DISJOB_SCRIPT_CLASSPATH));

            JdbcTemplate jdbcTemplate = DBUtils.createJdbcTemplate("jdbc:mysql://localhost:3306/" + DB_NAME, USERNAME, PASSWORD);

            System.out.println("\n--------------------------------------------------------testMysql");
            DBUtils.testMysqlVersion(jdbcTemplate);

            System.out.println("\n--------------------------------------------------------testJdbcTemplate");
            DBUtils.testJdbcTemplate(jdbcTemplate);

            System.out.println("\n--------------------------------------------------------testQuerySql");
            DBUtils.testQuerySchedJob(jdbcTemplate);


            System.out.println("Embedded docker mysql started!");
            new CountDownLatch(1).await();
        }
    }

}

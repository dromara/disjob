/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.test.db;

import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
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
 * docker pull mysql/mysql-server:8.0.31
 *
 * SELECT VERSION()  ->  8.0.27
 *
 * dependency maven junit:junit
 *
 * username: root
 * password: 无需密码
 * </pre>
 *
 * @author Ponfee
 */
public class EmbeddedMysqlServerTestcontainers {
    private static final List<String> PORT_BINDINGS = Collections.singletonList("3306:3306");

    public static void main(String[] args) throws Exception {
        DockerImageName dockerImage = DockerImageName.parse("mysql/mysql-server:8.0.31").asCompatibleSubstituteFor("mysql");
        try (MySQLContainer<?> mySQLContainer = new MySQLContainer<>(dockerImage)
            //.withConfigurationOverride("mysql_conf_override") // resource file: “resources/mysql_conf_override/my.cnf”
            .withPrivilegedMode(true)
            .withUsername("root")
            .withPassword("")
            .withDatabaseName("test")
            .withEnv("MYSQL_ROOT_HOST", "%")
            //.withInitScript(DISJOB_SCRIPT_PATH)
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(EmbeddedMysqlServerTestcontainers.class)))
        ) {
            mySQLContainer.setPortBindings(PORT_BINDINGS);
            Runtime.getRuntime().addShutdownHook(new Thread(mySQLContainer::close));

            System.out.println("Embedded docker mysql starting...");
            mySQLContainer.start();
            System.out.println("Embedded docker mysql started!");
            //mySQLContainer.execInContainer("mysqld --skip-grant-tables");

            // scriptPath只用于打印日志，此处直接设置为空字符串
            String scriptPath = "";
            String jdbcUrlParameter = "?useSSL=false&connectTimeout=2000&socketTimeout=5000";
            ScriptUtils.executeDatabaseScript(new JdbcDatabaseDelegate(mySQLContainer, jdbcUrlParameter), scriptPath, DBUtils.loadScript(DISJOB_ADMIN_SCRIPT_CLASSPATH));
            ScriptUtils.executeDatabaseScript(new JdbcDatabaseDelegate(mySQLContainer, jdbcUrlParameter), scriptPath, DBUtils.loadScript(DISJOB_SCRIPT_CLASSPATH));

            JdbcTemplate jdbcTemplate = DBUtils.createJdbcTemplate("jdbc:mysql://localhost:3306/" + DB_NAME, USERNAME, PASSWORD);

            System.out.println("\n--------------------------------------------------------testMysql");
            DBUtils.testMysqlVersion(jdbcTemplate);

            System.out.println("\n--------------------------------------------------------testJdbcTemplate");
            DBUtils.testJdbcTemplate(jdbcTemplate);

            System.out.println("\n--------------------------------------------------------testQuerySql");
            DBUtils.testQuerySchedJob(jdbcTemplate);

            new CountDownLatch(1).await();
        }
    }

}

/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.test.db;

import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static cn.ponfee.scheduler.test.db.DBUtils.DB_NAME;

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
            .withInitScript(DBUtils.DB_SCRIPT_PATH) // IOUtils.resourceToString(script-path, UTF_8, DBTools.class.getClassLoader())
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(EmbeddedMysqlServerTestcontainers.class)))
        ) {
            mySQLContainer.setPortBindings(PORT_BINDINGS);
            Runtime.getRuntime().addShutdownHook(new Thread(mySQLContainer::close));

            System.out.println("Embedded docker mysql starting...");
            mySQLContainer.start();
            System.out.println("Embedded docker mysql started!");
            //mySQLContainer.execInContainer("mysqld --skip-grant-tables");

            /*
            // the script-path only use for log, so here can set to an empty string value
            String scriptPath = "";
            String jdbcUrlParameter = "?useSSL=false&connectTimeout=2000&socketTimeout=5000";
            String scriptContent = DBTools.loadScript();
            ScriptUtils.executeDatabaseScript(new JdbcDatabaseDelegate(mySQLContainer, jdbcUrlParameter), scriptPath, scriptContent);
            */

            JdbcTemplate jdbcTemplate = DBUtils.createJdbcTemplate("jdbc:mysql://localhost:3306/" + DB_NAME, DB_NAME, DB_NAME);

            System.out.println("\n--------------------------------------------------------testMysql");
            DBUtils.testMysql(jdbcTemplate);

            System.out.println("\n--------------------------------------------------------testJdbcTemplate");
            DBUtils.testJdbcTemplate(jdbcTemplate);

            System.out.println("\n--------------------------------------------------------testQuerySql");
            DBUtils.testQuerySchedJob(jdbcTemplate);

            new CountDownLatch(1).await();
        }
    }

}

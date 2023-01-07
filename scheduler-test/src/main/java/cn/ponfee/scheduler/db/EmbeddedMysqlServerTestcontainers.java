/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.db;

import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static cn.ponfee.scheduler.db.DBTools.DB_NAME;

/**
 * <pre>
 * Docker mysql for testcontainers
 *
 * docker pull mysql/mysql-server:8.0.31
 *
 * SELECT VERSION()  ->  8.0.27
 * </pre>
 *
 * @author Ponfee
 */
public class EmbeddedMysqlServerTestcontainers {
    private static final List<String> PORT_BINDINGS = Arrays.asList("3306:3306");

    public static void main(String[] args) {
        DockerImageName dockerImage = DockerImageName.parse("mysql/mysql-server:8.0.31").asCompatibleSubstituteFor("mysql");
        try (MySQLContainer mySQLContainer = new MySQLContainer<>(dockerImage)
            //.withConfigurationOverride("mysql_conf_override")
            .withPrivilegedMode(true)
            .withUsername("root")
            .withPassword("")
            .withDatabaseName("test")
            .withEnv("MYSQL_ROOT_HOST", "%")
            //.withInitScript(scriptPath)
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(EmbeddedMysqlServerTestcontainers.class)))
        ) {
            mySQLContainer.setPortBindings(PORT_BINDINGS);
            Runtime.getRuntime().addShutdownHook(new Thread(mySQLContainer::close));

            System.out.println("Embedded docker mysql starting...");
            mySQLContainer.start();
            System.out.println("Embedded docker mysql started!");
            //mySQLContainer.execInContainer("mysqld --skip-grant-tables");
            ScriptUtils.executeDatabaseScript(new JdbcDatabaseDelegate(mySQLContainer, ""), "", DBTools.loadScript());
            JdbcTemplate jdbcTemplate = DBTools.createJdbcTemplate("jdbc:mysql://localhost:3306/" + DB_NAME, DB_NAME, DB_NAME);

            System.out.println("\n\n--------------------------------------------------------testMysql");
            DBTools.testMysql(jdbcTemplate);

            System.out.println("\n\n--------------------------------------------------------testJdbcTemplate");
            DBTools.testJdbcTemplate(jdbcTemplate);

            System.out.println("\n\n--------------------------------------------------------testQuerySql");
            DBTools.testQuerySchedJob(jdbcTemplate);

            new CountDownLatch(1).await();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

package cn.ponfee.scheduler.db;

import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfiguration;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import cn.ponfee.scheduler.common.base.exception.CheckedThrowing;
import cn.ponfee.scheduler.common.util.Files;
import cn.ponfee.scheduler.common.util.MavenProjects;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * MariaDB Server
 * <p>SELECT VERSION()  ->  10.2.11-MariaDB
 *
 * @author Ponfee
 */
public class EmbeddedMysqlServerMariaDB {

    public static void main(String[] args) throws Exception {
        String dbName = "distributed_scheduler";
        String dataDir = createDataDirectory();

        DBConfiguration configuration = DBConfigurationBuilder.newBuilder()
            .setPort(3306) // OR, default: setPort(0); => autom. detect free port
            .setDataDir(dataDir) // just an example
            //.addArg("--skip-grant-tables") // 默认就是skip-grant-tables
            .build();
        DB db = DB.newEmbeddedDB(configuration);
        Runtime.getRuntime().addShutdownHook(new Thread(CheckedThrowing.runnable(db::stop)));

        try {
            System.out.println("Embedded maria db starting...");
            db.start();
            System.out.println("Embedded maria db started!");

            db.source(IOUtils.toInputStream(loadScript(), StandardCharsets.UTF_8));
            JdbcTemplate jdbcTemplate = DBTools.createJdbcTemplate("jdbc:mysql://localhost:3306/" + dbName, dbName, dbName);

            System.out.println("\n\n--------------------------------------------------------testDatabase");
            DBTools.testNativeConnection("com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost:3306/" + dbName, dbName, dbName);

            System.out.println("\n\n--------------------------------------------------------testMysql");
            DBTools.testMysql(jdbcTemplate);

            System.out.println("\n\n--------------------------------------------------------testJdbcTemplate");
            DBTools.testJdbcTemplate(jdbcTemplate);

            System.out.println("\n\n--------------------------------------------------------testQuerySql");
            DBTools.testQuerySchedJob(jdbcTemplate);

            new CountDownLatch(1).await();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.stop();
        }
    }

    private static String loadScript() throws Exception {
        return Arrays.stream(DBTools.loadScript().split("\n"))
            .filter(s -> !StringUtils.startsWithAny(s, "CREATE USER ", "GRANT ALL PRIVILEGES ON ", "FLUSH PRIVILEGES;")) // The MariaDB server is running with the --skip-grant-tables option so it cannot execute this statement
            .collect(Collectors.joining("\n"));
    }

    private static String createDataDirectory() throws IOException {
        String dataDir = MavenProjects.getProjectBaseDir() + "/target/mariadb/";
        File file = new File(dataDir);
        if (file.exists()) {
            PathUtils.deleteDirectory(file.toPath());
        }
        Files.mkdir(file);
        return dataDir;
    }

}

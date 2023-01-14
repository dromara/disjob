/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.test.db;

import cn.ponfee.scheduler.common.util.Files;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.common.util.MavenProjects;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.file.PathUtils;
import org.h2.server.web.DbStarter;
import org.h2.tools.RunScript;
import org.junit.Assert;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * H2 database server
 *
 * @author Ponfee
 */
public class EmbeddedH2DatabaseServer {

    public static void main(String[] args) throws Exception {
        String jdbcUrl = buildJdbcUrl("test");
        String username = "sa", password = "";
        System.out.println("Embedded h2 database starting...");
        //new JakartaDbStarter(); // error
        new DbStarter();
        //new WebServer().start();
        //new TcpServer().start();
        //new PgServer().start();
        System.out.println("Embedded h2 database started!");

        JdbcTemplate jdbcTemplate = DBTools.createJdbcTemplate(jdbcUrl, username, password);

        System.out.println("\n--------------------------------------------------------testDatabase");
        DBTools.testNativeConnection("org.h2.Driver", jdbcUrl, username, password);

        System.out.println("\n--------------------------------------------------------testJdbcTemplate");
        DBTools.testJdbcTemplate(jdbcTemplate);

        System.out.println("\n--------------------------------------------------------testScript");
        testScript(jdbcTemplate);

        new CountDownLatch(1).await();
    }

    private static String buildJdbcUrl(String dbName) throws IOException {
        String dataDir = MavenProjects.getProjectBaseDir() + "/target/h2/";

        File file = new File(dataDir);
        if (file.exists()) {
            PathUtils.deleteDirectory(file.toPath());
        }
        Files.mkdir(file);
        return "jdbc:h2:" + dataDir + dbName;
    }

    private static void testScript(JdbcTemplate jdbcTemplate) {
        String scriptPath = MavenProjects.getProjectBaseDir() + "/src/test/DB/H2/H2_SCRIPT.sql";

        // 加载脚本方式一：
        //jdbcTemplate.execute("RUNSCRIPT FROM '" + scriptPath + "'");

        // 加载脚本方式二：
        //jdbcTemplate.execute(IOUtils.toString(new FileInputStream(scriptPath), StandardCharsets.UTF_8));

        // 加载脚本方式三：
        jdbcTemplate.execute((ConnectionCallback<Void>) conn -> {
            try {
                String script = IOUtils.toString(new FileInputStream(scriptPath), StandardCharsets.UTF_8);
                RunScript.execute(conn, new StringReader(script));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        });

        List<Map<String, Object>> result = jdbcTemplate.queryForList("SELECT * FROM test1");
        Assert.assertEquals("ae452457b1df438fa441e5640b162da6", result.get(0).get("NAME"));
        System.out.println("Query result: " + Jsons.toJson(result));
    }

}

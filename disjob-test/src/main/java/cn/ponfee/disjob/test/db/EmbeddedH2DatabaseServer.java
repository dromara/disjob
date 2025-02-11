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

import cn.ponfee.disjob.common.concurrent.ShutdownHookManager;
import cn.ponfee.disjob.common.util.Files;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.common.util.MavenProjects;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.h2.server.TcpServer;
import org.h2.tools.RunScript;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * H2 database server
 * <p>
 * 内存中（私有）      -> jdbc:h2:mem:;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MYSQL
 * 内存中（命名）      -> jdbc:h2:mem:<databaseName>;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MYSQL
 * 嵌入式（本地）连接   -> jdbc:h2:[file:][<data-dir-path>]<databaseName>
 *
 * @author Ponfee
 */
public class EmbeddedH2DatabaseServer {

    public static void main(String[] args) throws Exception {
        // jdbc:h2:/Users/ponfee/scm/github/disjob/disjob-test/target/h2/test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MYSQL
        String jdbcUrl = buildJdbcUrl("test");
        String username = "sa", password = "";

        System.out.println("Embedded h2 database starting...");
        //new org.h2.server.web.JakartaDbStarter(); // error: need dependency servlet-api
        //new org.h2.server.web.DbStarter(); // error: need dependency servlet-api
        TcpServer tcpServer = new TcpServer();
        ShutdownHookManager.addShutdownHook(Integer.MAX_VALUE, tcpServer::stop);
        tcpServer.start();
        //new org.h2.server.web.WebServer().start();
        //new org.h2.server.pg.PgServer().start();

        JdbcTemplate jdbcTemplate = DBUtils.createJdbcTemplate(jdbcUrl, username, password);

        System.out.println("\n--------------------------------------------------------testDatabase");
        DBUtils.testNativeConnection("org.h2.Driver", jdbcUrl, username, password);

        System.out.println("\n--------------------------------------------------------testJdbcTemplate");
        DBUtils.testJdbcTemplate(jdbcTemplate);

        System.out.println("\n--------------------------------------------------------testScript");
        testScript(jdbcTemplate);
        System.out.println("Embedded h2 database started!");

        new CountDownLatch(1).await();
    }

    private static String buildJdbcUrl(String dbName) throws IOException {
        String dataDir = MavenProjects.getProjectBaseDir() + "/target/h2";
        Files.cleanOrMakeDir(dataDir);
        return "jdbc:h2:" + dataDir + "/" + dbName + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MYSQL";
    }

    private static void testScript(JdbcTemplate jdbcTemplate) {
        String scriptPath = MavenProjects.getProjectBaseDir() + "/src/main/DB/H2/H2_SCRIPT.sql";

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
                throw new RuntimeException(e);
            }
            return null;
        });

        List<Map<String, Object>> result = jdbcTemplate.queryForList("SELECT * FROM test1");
        String expect = MapUtils.getString(result.get(0), "NAME");
        String actual = MapUtils.getString(result.get(0), "NAME");
        Assert.isTrue(expect.equals(actual), () -> expect + " != " + actual);

        System.out.println("Query result: " + Jsons.toJson(result));
    }

}

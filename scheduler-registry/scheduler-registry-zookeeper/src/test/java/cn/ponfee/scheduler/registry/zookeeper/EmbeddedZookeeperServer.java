/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.registry.zookeeper;

import cn.ponfee.scheduler.common.base.exception.Throwables;
import cn.ponfee.scheduler.common.util.MavenProjects;
import org.apache.curator.test.TestingServer;

import java.io.File;

/**
 * Embedded zooKeeper server.
 *
 * @author Ponfee
 */
public final class EmbeddedZookeeperServer {

    public static void main(String[] args) throws Exception {
        System.out.println("Embedded zookeeper server starting...");
        TestingServer testingServer = new TestingServer(2181, createTempDir());
        System.out.println("Embedded zookeeper server started!");

        Runtime.getRuntime().addShutdownHook(new Thread(Throwables.runnable(testingServer::stop)));
    }

    private static File createTempDir() {
        String path = String.format(MavenProjects.getProjectBaseDir() + "/target/zookeeper/data/%d/", System.nanoTime());
        File file = new File(path);
        file.mkdirs();
        return file;
    }

}

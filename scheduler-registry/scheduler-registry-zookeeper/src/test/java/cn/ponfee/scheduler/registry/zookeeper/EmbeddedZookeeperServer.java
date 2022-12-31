package cn.ponfee.scheduler.registry.zookeeper;

import cn.ponfee.scheduler.common.base.exception.CheckedThrowing;
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

        Runtime.getRuntime().addShutdownHook(new Thread(CheckedThrowing.runnable(testingServer::stop)));
    }

    private static File createTempDir() {
        String path = String.format(MavenProjects.getProjectBaseDir() + "/target/zookeeper/data/%d/", System.nanoTime());
        File file = new File(path);
        file.mkdirs();
        return file;
    }

}

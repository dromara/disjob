package cn.ponfee.scheduler.samples.common.zk;

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
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Thread.sleep(1000L);
                testingServer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
        System.out.println("Embedded zookeeper server started!");
    }

    private static File createTempDir() {
        String path = String.format("target/zookeeper/data/%d/", System.nanoTime());
        File file = new File(path);
        file.mkdirs();
        return file;
    }

}

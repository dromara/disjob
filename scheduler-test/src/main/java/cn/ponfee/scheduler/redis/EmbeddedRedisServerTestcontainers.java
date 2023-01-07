/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.redis;

import org.junit.Assert;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Embedded redis server.
 *
 * <pre>
 *   docker pull redis:6.2.8
 *
 *   docker run --name test_redis \
 *     --privileged=true \
 *     -p 6379:6379 \
 *     -d redis:6.2.8
 * </pre>
 *
 * @author Ponfee
 */
public final class EmbeddedRedisServerTestcontainers {

    private static final String NACOS_DOCKER_IMAGE_NAME = "redis:6.2.8";
    private static final List<String> PORT_BINDINGS = Arrays.asList("6379:6379");

    public static void main(String[] args) {
        DockerImageName consulImage = DockerImageName.parse(NACOS_DOCKER_IMAGE_NAME).asCompatibleSubstituteFor("redis-test");

        // --name: DockerImageName
        // --privileged: withPrivilegedMode
        // -p: setPortBindings
        // -v: withFileSystemBind
        // -e: withEnv
        GenericContainer dockerRedisContainer = new GenericContainer(consulImage)
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(EmbeddedRedisServerTestcontainers.class)))
            // 挂载映射文件非必需
            //.withFileSystemBind("/opt/docker/redis", "/usr/local/etc/redis", BindMode.READ_ONLY)
            //.withFileSystemBind("/opt/docker/redis/data", "/usr/local/etc/redis/data", BindMode.READ_WRITE)
            .withPrivilegedMode(true);
        dockerRedisContainer.setPortBindings(PORT_BINDINGS);
        Runtime.getRuntime().addShutdownHook(new Thread(dockerRedisContainer::close));

        try {
            System.out.println("Embedded docker redis server starting...");
            dockerRedisContainer.start();
            System.out.println("Embedded docker redis server started!");

            dockerRedisContainer.execInContainer("redis-cli", "config set requirepass 123456");

            Assert.assertEquals(PORT_BINDINGS, dockerRedisContainer.getPortBindings());
            new CountDownLatch(1).await();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dockerRedisContainer.close();
        }
    }

}

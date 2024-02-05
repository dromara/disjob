/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.test.redis;

import cn.ponfee.disjob.common.util.Jsons;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Embedded redis server.
 *
 * <pre>
 *   docker pull redis:7.2.4
 *
 *   docker run --name test_redis \
 *     --privileged=true \
 *     -p 6379:6379 \
 *     -d redis:7.2.4
 *
 * username: 无需用户名
 * password: 123456
 * </pre>
 *
 * <a href="https://hub.docker.com/_/redis/tags">docker官网查看版本</a>
 *
 * @author Ponfee
 */
public final class EmbeddedRedisServerTestcontainers {

    private static final String NACOS_DOCKER_IMAGE_NAME = "redis:7.2.4";
    private static final List<String> PORT_BINDINGS = Collections.singletonList("6379:6379/tcp");

    public static void main(String[] args) {
        DockerImageName redisImage = DockerImageName.parse(NACOS_DOCKER_IMAGE_NAME).asCompatibleSubstituteFor("redis-test");

        // --name: DockerImageName
        // --privileged: withPrivilegedMode
        // -p: setPortBindings
        // -v: withFileSystemBind
        // -e: withEnv
        GenericContainer<?> dockerRedisContainer = new GenericContainer<>(redisImage)
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
            dockerRedisContainer.execInContainer("redis-cli", "config set requirepass 123456");
            Assert.isTrue(
                CollectionUtils.isEqualCollection(PORT_BINDINGS, dockerRedisContainer.getPortBindings()),
                () -> Jsons.toJson(PORT_BINDINGS) + "!=" + Jsons.toJson(dockerRedisContainer.getPortBindings())
            );
            // Assertions.assertThat(dockerRedisContainer.getPortBindings()).hasSameElementsAs(PORT_BINDINGS);
            System.out.println("Embedded docker redis server started!");

            new CountDownLatch(1).await();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dockerRedisContainer.close();
        }
    }

}

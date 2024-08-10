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
 *   docker pull redis:7.4.0
 *
 *   docker run --name test_redis \
 *     --privileged=true \
 *     -p 6379:6379 \
 *     -d redis:7.4.0
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

    private static final String NACOS_DOCKER_IMAGE_NAME = "redis:7.4.0";
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
            Assert.isTrue(dockerRedisContainer.isCreated(), "Created error.");
            Assert.isTrue(dockerRedisContainer.isRunning(), "Running error.");
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

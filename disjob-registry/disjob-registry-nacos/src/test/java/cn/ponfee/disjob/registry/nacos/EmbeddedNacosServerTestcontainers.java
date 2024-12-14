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

package cn.ponfee.disjob.registry.nacos;

import org.assertj.core.api.Assertions;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Embedded nacos server.
 * <p><a href="https://nacos.io/zh-cn/docs/quick-start.html">Nacos</a>
 *
 *
 * <pre>
 *   docker pull nacos/nacos-server:v2.4.0.1-slim
 *
 *   mkdir -p /opt/docker/nacos/init.d /opt/docker/nacos/logs
 *   touch /opt/docker/nacos/init.d/custom.properties
 *   echo "management.endpoints.web.exposure.include=*" > /opt/docker/nacos/init.d/custom.properties
 *
 *   # --platform linux/amd64 \
 *   # --platform linux/arm64 \
 *   docker run -d \
 *     --name nacos-quick \
 *     --privileged=true \
 *     --restart unless-stopped \
 *     -p 8848:8848 \
 *     -p 8849:8849 \
 *     -p 9848:9848 \
 *     -p 9849:9849 \
 *     -e JVM_XMS=256m \
 *     -e JVM_XMX=256m \
 *     -e MODE=standalone \
 *     -e PREFER_HOST_MODE=hostname \
 *     -v /opt/docker/nacos/init.d/custom.properties:/home/nacos/init.d/custom.properties \
 *     -v /opt/docker/nacos/logs:/home/nacos/logs \
 *     nacos/nacos-server:v2.4.0.1-slim
 *
 *     # 初始账号密码都为nacos
 *     # http://localhost:8848/nacos
 * </pre>
 *
 * <a href="https://hub.docker.com/r/nacos/nacos-server/tags">docker官网查看版本</a>
 *
 * @author Ponfee
 */
public final class EmbeddedNacosServerTestcontainers {

    private static final String NACOS_DOCKER_IMAGE_NAME = "nacos/nacos-server:v2.4.0.1-slim";
    private static final List<String> PORT_BINDINGS = Arrays.asList("8848:8848/tcp", "8849:8849/tcp", "9848:9848/tcp", "9849:9849/tcp");

    public static void main(String[] args) throws Exception {
        DockerImageName nacosImage = DockerImageName.parse(NACOS_DOCKER_IMAGE_NAME).asCompatibleSubstituteFor("nacos-test");

        // --name: DockerImageName
        // --privileged: withPrivilegedMode
        // -p: setPortBindings
        // -v: withFileSystemBind
        // -e: withEnv
        GenericContainer<?> dockerNacosContainer = new GenericContainer<>(nacosImage)
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(EmbeddedNacosServerTestcontainers.class)))
            // 挂载映射文件非必需
            //.withFileSystemBind("/opt/docker/nacos/init.d/custom.properties", "/home/nacos/init.d/custom.properties", BindMode.READ_ONLY)
            //.withFileSystemBind("/opt/docker/nacos/logs", "/home/nacos/logs", BindMode.READ_WRITE)
            .withPrivilegedMode(true)
            .withEnv("MODE", "standalone")
            .withEnv("PREFER_HOST_MODE", "hostname")
            .withEnv("JVM_XMS", "256m")
            .withEnv("JVM_XMX", "256m");

        dockerNacosContainer.setPortBindings(PORT_BINDINGS);
        Runtime.getRuntime().addShutdownHook(new Thread(dockerNacosContainer::close));

        try {
            System.out.println("Embedded docker nacos server starting...");
            dockerNacosContainer.start();
            Assertions.assertThat(dockerNacosContainer.isCreated()).isTrue();
            Assertions.assertThat(dockerNacosContainer.isRunning()).isTrue();
            Assertions.assertThat(dockerNacosContainer.getPortBindings()).hasSameElementsAs(PORT_BINDINGS);
            System.out.println("Embedded docker nacos server started!");

            new CountDownLatch(1).await();
        } finally {
            dockerNacosContainer.close();
        }
    }

}

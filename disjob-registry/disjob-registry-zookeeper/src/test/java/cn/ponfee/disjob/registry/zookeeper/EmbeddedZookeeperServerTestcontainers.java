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

package cn.ponfee.disjob.registry.zookeeper;

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
 * Embedded zookeeper server.
 * docker pull zookeeper:3.9.3
 *
 * <a href="https://hub.docker.com/_/zookeeper/tags">docker官网查看版本</a>
 *
 * @author Ponfee
 */
public final class EmbeddedZookeeperServerTestcontainers {

    private static final String NACOS_DOCKER_IMAGE_NAME = "zookeeper:3.9.3";
    private static final List<String> PORT_BINDINGS = Collections.singletonList("2181:2181/tcp");

    public static void main(String[] args) throws Exception {
        DockerImageName image = DockerImageName.parse(NACOS_DOCKER_IMAGE_NAME).asCompatibleSubstituteFor("zookeeper-test");
        try (
            // --name: DockerImageName
            // --privileged: withPrivilegedMode
            // -p: setPortBindings
            // -v: withFileSystemBind
            // -e: withEnv
            GenericContainer<?> container = new GenericContainer<>(image)
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(EmbeddedZookeeperServerTestcontainers.class)))
                // 挂载映射文件非必需
                //.withFileSystemBind("/opt/docker/zookeeper", "/usr/local/etc/zookeeper", BindMode.READ_ONLY)
                //.withFileSystemBind("/opt/docker/zookeeper/data", "/usr/local/etc/zookeeper/data", BindMode.READ_WRITE)
                .withPrivilegedMode(true)
        ) {
            container.setPortBindings(PORT_BINDINGS);

            System.out.println("Embedded docker zookeeper server starting...");
            container.start();

            if (!CollectionUtils.isEqualCollection(PORT_BINDINGS, container.getPortBindings())) {
                throw new IllegalStateException(PORT_BINDINGS + "!=" + container.getPortBindings());
            }
            Assert.isTrue(container.isCreated(), "Created error.");
            Assert.isTrue(container.isRunning(), "Running error.");

            System.out.println("Embedded docker zookeeper server started!");
            new CountDownLatch(1).await();
        }
    }

}

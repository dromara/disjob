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

package cn.ponfee.disjob.registry.consul;

import org.assertj.core.api.Assertions;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * <pre>
 * Embedded consul server based testcontainers.
 * <a href="https://www.testcontainers.org/modules/consul/">testcontainers consul</a>
 * 1、startup local docker environment
 * 2、pull docker consul image: docker pull hashicorp/consul:1.20.1
 * 3、"hashicorp/consul:1.20.1" is docker {image-name:version}
 *
 * Other:
 *  本地搜索：docker search consul --limit 20
 *
 * 查看latest的具体版本号：docker image inspect {image-name}:latest | grep -i version
 * </pre>
 *
 * <a href="https://hub.docker.com/r/hashicorp/consul">docker官网查看版本</a>
 *
 * @author Ponfee
 */
public final class EmbeddedConsulServerTestcontainers {

    private static final String CONSUL_DOCKER_IMAGE_NAME = "hashicorp/consul:1.20.1";
    private static final List<String> PORT_BINDINGS = Arrays.asList("8500:8500/tcp", "8502:8502/tcp");

    public static void main(String[] args) throws Exception {
        String key = "config/testing1", val = "value123";
        DockerImageName consulImage = DockerImageName.parse(CONSUL_DOCKER_IMAGE_NAME).asCompatibleSubstituteFor("consul-test");

        try (
            ConsulContainer consulContainer = new ConsulContainer(consulImage)
                .withConsulCommand("kv put " + key + " " + val)
        ) {
            consulContainer.setPortBindings(PORT_BINDINGS);

            System.out.println("Embedded docker consul server starting...");
            consulContainer.start();
            Assertions.assertThat(consulContainer.isCreated()).isTrue();
            Assertions.assertThat(consulContainer.isRunning()).isTrue();
            Assertions.assertThat(consulContainer.getPortBindings()).hasSameElementsAs(PORT_BINDINGS);
            Assertions.assertThat(consulContainer.getExposedPorts()).hasSameElementsAs(Arrays.asList(8500, 8502));
            Assertions.assertThat(consulContainer.execInContainer("consul", "kv", "get", key).getStdout().trim()).isEqualTo(val);
            System.out.println("Embedded docker consul server started!");

            new CountDownLatch(1).await();
        }
    }

}

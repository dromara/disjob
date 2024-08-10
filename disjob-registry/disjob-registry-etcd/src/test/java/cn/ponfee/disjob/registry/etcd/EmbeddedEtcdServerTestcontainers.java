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

package cn.ponfee.disjob.registry.etcd;

import io.etcd.jetcd.launcher.Etcd;
import io.etcd.jetcd.launcher.EtcdCluster;
import org.assertj.core.api.Assertions;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * <pre>
 * Embedded etcd server base testcontainers and docker.
 *
 * io.etcd:jetcd-launcher:0.7.7
 *
 * docker pull gcr.io/etcd-development/etcd:v3.5.15
 * </pre>
 *
 * <a href="https://github.com/etcd-io/etcd/releases">github官网查看版本</a>
 *
 * @author Ponfee
 */
public final class EmbeddedEtcdServerTestcontainers {

    private static final String ETCD_DOCKER_IMAGE_NAME = "gcr.io/etcd-development/etcd:v3.5.15";
    private static final List<String> PORT_BINDINGS = Arrays.asList("2379:2379/tcp", "2380:2380/tcp", "8080:8080/tcp");

    public static void main(String[] args) throws Exception {
        EtcdCluster etcd = Etcd.builder()
            .withImage(ETCD_DOCKER_IMAGE_NAME)
            .withClusterName(EmbeddedEtcdServerTestcontainers.class.getSimpleName())
            .withAdditionalArgs("--max-txn-ops", "1024")
            .build();

        etcd.containers().forEach(container -> {
            container.setPortBindings(PORT_BINDINGS);
            // other docker container settings
        });
        Runtime.getRuntime().addShutdownHook(new Thread(etcd::close));
        try {
            System.out.println("Embedded docker etcd server starting...");
            etcd.start();
            Assertions.assertThat(etcd.containers()).hasSize(1);
            Assertions.assertThat(etcd.containers().get(0).isCreated()).isTrue();
            Assertions.assertThat(etcd.containers().get(0).isRunning()).isTrue();
            Assertions.assertThat(etcd.containers().get(0).getPortBindings()).hasSameElementsAs(PORT_BINDINGS);
            System.out.println("Embedded docker etcd server started!");

            new CountDownLatch(1).await();
        } finally {
            etcd.close();
        }
    }

}

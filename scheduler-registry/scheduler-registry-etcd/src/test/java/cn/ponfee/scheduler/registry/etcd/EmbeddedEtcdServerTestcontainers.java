/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.registry.etcd;

import io.etcd.jetcd.launcher.Etcd;
import io.etcd.jetcd.launcher.EtcdCluster;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Embedded etcd server base testcontainers and docker.
 *
 * @author Ponfee
 */
public final class EmbeddedEtcdServerTestcontainers {

    private static final String ETCD_DOCKER_IMAGE_NAME = "gcr.io/etcd-development/etcd:v3.5.4";
    private static final List<String> PORT_BINDINGS = Arrays.asList("2379:2379", "2380:2380", "8080:8080");

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
            System.out.println("Embedded docker etcd server started!");
            new CountDownLatch(1).await();
        } finally {
            etcd.close();
        }
    }

}

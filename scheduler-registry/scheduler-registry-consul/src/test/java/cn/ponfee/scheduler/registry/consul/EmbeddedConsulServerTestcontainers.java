package cn.ponfee.scheduler.registry.consul;

import org.junit.jupiter.api.Assertions;
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
 * 2、pull docker consul image: docker pull consul
 * 3、"consul:latest" is docker {image-name:version}
 *
 * 查看latest的具体版本号：docker image inspect {image-name}:latest | grep -i version
 * </pre>
 *
 * @author Ponfee
 */
public final class EmbeddedConsulServerTestcontainers {

    private static final String CONSUL_DOCKER_IMAGE_NAME = "consul:1.14.2";
    private static final List<String> PORT_BINDINGS = Arrays.asList("8500:8500", "8502:8502");

    public static void main(String[] args) {
        System.out.println("Embedded consul server starting...");
        String key = "config/testing1", val = "value123";

        DockerImageName consulImage = DockerImageName.parse(CONSUL_DOCKER_IMAGE_NAME)
            .asCompatibleSubstituteFor("consul-test");

        ConsulContainer consulContainer = new ConsulContainer(consulImage)
            .withConsulCommand("kv put " + key + " " + val);

        consulContainer.setPortBindings(PORT_BINDINGS);

        try {
            Runtime.getRuntime().addShutdownHook(new Thread(consulContainer::close));
            consulContainer.start();

            Assertions.assertEquals(PORT_BINDINGS, consulContainer.getPortBindings());
            Assertions.assertEquals(Arrays.asList(8500, 8502), consulContainer.getExposedPorts());
            Assertions.assertEquals(val, consulContainer.execInContainer("consul", "kv", "get", key).getStdout().trim());

            System.out.println("Embedded consul server started!");
            new CountDownLatch(1).await();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            consulContainer.close();
        }
    }

}

package cn.ponfee.scheduler.registry.nacos;

import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
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
 *   #docker pull nacos/nacos-server
 *   docker pull zhusaidong/nacos-server-m1:2.0.3
 *
 *   mkdir -p /opt/docker/nacos/init.d /opt/docker/nacos/logs
 *   touch /opt/docker/nacos/init.d/custom.properties
 *   echo "management.endpoints.web.exposure.include=*" > /opt/docker/nacos/init.d/custom.properties
 *
 *   # --platform linux/amd64 \
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
 *     zhusaidong/nacos-server-m1:2.0.3
 *
 *     # 初始账号密码都为nacos
 *     # http://localhost:8848/nacos
 * </pre>
 *
 * @author Ponfee
 */
public final class EmbeddedNacosServer {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedNacosServer.class);

    private static final String NACOS_DOCKER_IMAGE_NAME = "zhusaidong/nacos-server-m1:2.0.3";
    private static final List<String> PORT_BINDINGS = Arrays.asList("8848:8848", "8849:8849", "9848:9848", "9849:9849");

    public static void main(String[] args) {
        System.out.println("Embedded nacos server starting...");

        DockerImageName consulImage = DockerImageName.parse(NACOS_DOCKER_IMAGE_NAME)
            .asCompatibleSubstituteFor("nacos-test");

        // --name: DockerImageName
        // --privileged: withPrivilegedMode
        // -p: setPortBindings
        // -v: withFileSystemBind
        // -e: withEnv
        GenericContainer nacosDockerContainer = new GenericContainer(consulImage)
            .withLogConsumer(new Slf4jLogConsumer(LOG))
            // 挂载映射文件非必需
            //.withFileSystemBind("/opt/docker/nacos/init.d/custom.properties", "/home/nacos/init.d/custom.properties", BindMode.READ_ONLY)
            //.withFileSystemBind("/opt/docker/nacos/logs", "/home/nacos/logs", BindMode.READ_WRITE)
            .withPrivilegedMode(true)
            .withEnv("MODE", "standalone")
            .withEnv("PREFER_HOST_MODE", "hostname")
            .withEnv("JVM_XMS", "256m")
            .withEnv("JVM_XMX", "256m");

        nacosDockerContainer.setPortBindings(PORT_BINDINGS);

        try {
            Runtime.getRuntime().addShutdownHook(new Thread(nacosDockerContainer::close));
            nacosDockerContainer.start();

            Assertions.assertEquals(PORT_BINDINGS, nacosDockerContainer.getPortBindings());
            //Assertions.assertEquals(Arrays.asList(8848, 8849, 9848, 9849), nacosDockerContainer.getExposedPorts());

            System.out.println("Embedded nacos server started!");
            new CountDownLatch(1).await();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            nacosDockerContainer.close();
        }
    }

}

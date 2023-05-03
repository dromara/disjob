/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.registry.consul;

import cn.ponfee.disjob.common.util.MavenProjects;
import com.pszymczyk.consul.ConsulProcess;
import com.pszymczyk.consul.ConsulStarterBuilder;
import com.pszymczyk.consul.infrastructure.HttpBinaryRepository;

import java.io.File;
import java.nio.file.Path;

/**
 * Embedded consul server based pszymczyk.
 * <p><a href="https://github.com/pszymczyk/embedded-consul">github embedded consul</a>
 *
 * @author Ponfee
 */
public final class EmbeddedConsulServerPszymczyk {

    public static void main(String[] args) {
        System.setProperty(HttpBinaryRepository.CONSUL_BINARY_CDN, "https://releases.hashicorp.com/consul/");

        System.out.println("Embedded pszymczyk consul server starting...");
        ConsulProcess consul = ConsulStarterBuilder.consulStarter()
            .withConsulVersion("1.14.2")
            .withConsulBinaryDownloadDirectory(createConsulBinaryDownloadDirectory())
            .withHttpPort(8500)
            .buildAndStart();
        System.out.println("Embedded pszymczyk consul server started!");

        Runtime.getRuntime().addShutdownHook(new Thread(consul::close));
    }

    private static Path createConsulBinaryDownloadDirectory() {
        File file = new File(MavenProjects.getProjectBaseDir() + "/target/consul/");
        file.mkdirs();
        return file.toPath();
    }

}

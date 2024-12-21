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

import cn.ponfee.disjob.common.util.Files;
import cn.ponfee.disjob.common.util.MavenProjects;
import com.pszymczyk.consul.ConsulProcess;
import com.pszymczyk.consul.ConsulStarterBuilder;
import com.pszymczyk.consul.infrastructure.HttpBinaryRepository;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Embedded consul server based pszymczyk.
 * <p><a href="https://github.com/pszymczyk/embedded-consul">github embedded consul</a>
 *
 * @author Ponfee
 */
public final class EmbeddedConsulServerPszymczyk {

    public static void main(String[] args) throws IOException {
        System.setProperty(HttpBinaryRepository.CONSUL_BINARY_CDN, "https://releases.hashicorp.com/consul/");
        System.out.println("Embedded pszymczyk consul server starting...");

        ConsulProcess consul = ConsulStarterBuilder.consulStarter()
            .withConsulVersion("1.20.1")
            .withConsulBinaryDownloadDirectory(createConsulBinaryDownloadDirectory())
            .withHttpPort(8500)
            .buildAndStart();
        Runtime.getRuntime().addShutdownHook(new Thread(consul::close));
        System.out.println("------------ http://127.0.0.1:8500 ------------");

        System.out.println("Embedded pszymczyk consul server started!");
    }

    private static Path createConsulBinaryDownloadDirectory() throws IOException {
        String path = MavenProjects.getProjectBaseDir() + "/src/bin/consul";
        return Files.mkdirIfNotExists(path).toPath();
    }

}

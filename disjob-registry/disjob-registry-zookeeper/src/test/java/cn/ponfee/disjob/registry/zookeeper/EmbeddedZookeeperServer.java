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

import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.util.Files;
import cn.ponfee.disjob.common.util.MavenProjects;
import org.apache.curator.test.TestingServer;

import java.io.File;
import java.io.IOException;

/**
 * Embedded zooKeeper server.
 *
 * @author Ponfee
 */
public final class EmbeddedZookeeperServer {

    public static void main(String[] args) throws Exception {
        System.out.println("Embedded zookeeper server starting...");

        TestingServer testingServer = new TestingServer(2181, createTempDir());
        Runtime.getRuntime().addShutdownHook(new Thread(ThrowingRunnable.toCaught(testingServer::close)));

        System.out.println("Embedded zookeeper server started!");
    }

    private static File createTempDir() throws IOException {
        String path = MavenProjects.getProjectBaseDir() + "/target/zookeeper/data/" + System.nanoTime();
        return Files.cleanOrMakeDir(path);
    }

}

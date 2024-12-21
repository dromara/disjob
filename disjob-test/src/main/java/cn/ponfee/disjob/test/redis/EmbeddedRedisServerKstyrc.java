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

package cn.ponfee.disjob.test.redis;

import redis.embedded.RedisServer;

/**
 * Embedded redis server.
 * <p><a href="https://github.com/ponfee/embedded-redis">github embedded redis</a>
 * <p><a href="https://blog.csdn.net/qq_45565645/article/details/125052006">redis configuration1</a>
 * <p><a href="https://huaweicloud.csdn.net/633564b3d3efff3090b55531.html">redis configuration2</a>
 *
 * <p>username: 无需用户名
 * <p>password: 123456
 *
 * @author Ponfee
 */
public final class EmbeddedRedisServerKstyrc {

    public static void main(String[] args) {
        RedisServer redisServer = start(6379, 6380);
    }

    public static RedisServer start(int masterPort, int slavePort) {
        RedisServer redisServer = RedisServer.builder()
            //.redisExecProvider(customRedisProvider)
            .port(masterPort)
            .slaveOf("localhost", slavePort)
            .setting("requirepass 123456")

            // redis 6.0 ACL: https://blog.csdn.net/qq_29235677/article/details/121475204
            //   command: "ACL SETUSER username on >password ~<key-pattern> +@<category>"
            //   config file: "user username on >password ~<key-pattern> +@<category>"
            //.setting("ACL SETUSER test123 on >123456 ~* +@all")

            .setting("daemonize no")
            .setting("appendonly no")
            .setting("slave-read-only no")
            .setting("maxmemory 128M")
            .build();

        System.out.println("Embedded kstyrc redis server starting...");
        Runtime.getRuntime().addShutdownHook(new Thread(redisServer::stop));
        redisServer.start();
        System.out.println("Embedded kstyrc redis server started!");

        return redisServer;
    }

}

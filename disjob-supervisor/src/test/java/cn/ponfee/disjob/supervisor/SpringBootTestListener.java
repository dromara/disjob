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

package cn.ponfee.disjob.supervisor;

import cn.ponfee.disjob.common.spring.SpringUtils;
import cn.ponfee.disjob.common.util.NetUtils;
import cn.ponfee.disjob.test.EmbeddedMysqlAndRedisServer;
import com.google.common.collect.ImmutableMap;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;

import java.util.Map;

/**
 * Spring boot test application listener
 *
 * @author Ponfee
 */
public class SpringBootTestListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        int mysqlPort = NetUtils.findAvailablePort(5000);
        int redisPort = NetUtils.findAvailablePort(mysqlPort + 1);
        int redisSlavePort = NetUtils.findAvailablePort(redisPort + 1);

        Map<String, Object> ports = ImmutableMap.of("mysql.port", mysqlPort, "redis.port", redisPort);
        SpringUtils.addPropertySource(event.getEnvironment(), "server_test_port_config", ports);

        EmbeddedMysqlAndRedisServer.starter()
            .mysqlPort(mysqlPort)
            .redisMasterPort(redisPort)
            .redisSlavePort(redisSlavePort)
            .start();
    }

}

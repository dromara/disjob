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

package cn.ponfee.disjob.samples.worker.redis;

import lombok.experimental.SuperBuilder;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;

/**
 * Creates standalone spring redis template
 *
 * @author Ponfee
 */
@SuperBuilder
public class StandaloneRedisTemplateCreator extends AbstractRedisTemplateCreator {

    private String host;
    private int port;

    @Override
    protected RedisConfiguration createRedisConfiguration() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setDatabase(super.database);
        configuration.setUsername(super.username);
        configuration.setPassword(super.password);
        configuration.setHostName(host);
        configuration.setPort(port);
        return configuration;
    }

}

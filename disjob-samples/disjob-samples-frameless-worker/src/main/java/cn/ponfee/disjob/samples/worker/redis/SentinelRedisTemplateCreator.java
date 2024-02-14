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
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates sentinel spring redis template
 *
 * @author Ponfee
 */
@SuperBuilder
public class SentinelRedisTemplateCreator extends AbstractRedisTemplateCreator {

    private String sentinelMaster;
    private String sentinelNodes;
    private String sentinelUsername;
    private String sentinelPassword;

    @Override
    protected RedisConfiguration createRedisConfiguration() {
        RedisSentinelConfiguration configuration = new RedisSentinelConfiguration();
        configuration.setDatabase(super.database);
        configuration.setUsername(super.username);
        configuration.setPassword(super.password);
        configuration.setMaster(sentinelMaster);

        List<RedisNode> redisNodes = Arrays.stream(sentinelNodes.split(","))
            .filter(StringUtils::isNotBlank)
            .map(RedisNode::fromString)
            .collect(Collectors.toList());
        configuration.setSentinels(redisNodes);

        // Sentinel username/password
        configuration.setSentinelUsername(sentinelUsername);
        if (StringUtils.isNotEmpty(sentinelPassword)) {
            configuration.setSentinelPassword(RedisPassword.of(sentinelPassword));
        }

        return configuration;
    }

}

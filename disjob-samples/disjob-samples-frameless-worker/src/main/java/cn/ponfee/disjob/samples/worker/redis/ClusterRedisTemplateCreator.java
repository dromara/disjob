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
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConfiguration;

import java.util.Arrays;

/**
 * Creates cluster spring redis template
 *
 * @author Ponfee
 */
@SuperBuilder
public class ClusterRedisTemplateCreator extends AbstractRedisTemplateCreator {

    private String clusterNodes;
    private Integer clusterMaxRedirects;

    @Override
    protected RedisConfiguration createRedisConfiguration() {
        RedisClusterConfiguration configuration = new RedisClusterConfiguration(
            Arrays.asList(clusterNodes.split(","))
        );
        if (clusterMaxRedirects != null) {
            configuration.setMaxRedirects(clusterMaxRedirects);
        }
        configuration.setUsername(super.username);
        configuration.setPassword(super.password);
        return configuration;
    }

}

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

package cn.ponfee.disjob.registry.zookeeper.configuration;

import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.registry.AbstractRegistryProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Zookeeper registry configuration properties.
 *
 * @author Ponfee
 */
@Getter
@Setter
@ConfigurationProperties(prefix = JobConstants.DISJOB_REGISTRY_KEY_PREFIX + ".zookeeper")
public class ZookeeperRegistryProperties extends AbstractRegistryProperties {
    private static final long serialVersionUID = -8395535372974631095L;

    private String connectString = "localhost:2181";
    private String username;
    private String password;

    private int connectionTimeoutMs = 5 * 1000;
    private int sessionTimeoutMs = 60 * 1000;

    private int baseSleepTimeMs = 50;
    private int maxRetries = 10;
    private int maxSleepMs = 500;
    private int maxWaitTimeMs = 5000;

    public String authorization() {
        if (isEmpty(username)) {
            return isEmpty(password) ? null : ":" + password;
        }
        return username + ":" + (isEmpty(password) ? "" : password);
    }

}

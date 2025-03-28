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

package cn.ponfee.disjob.alert.configuration;

import cn.ponfee.disjob.alert.Alerter;
import cn.ponfee.disjob.common.base.ToJsonString;
import cn.ponfee.disjob.core.alert.AlertType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

/**
 * Abstract alert properties
 *
 * @author Ponfee
 */
@Setter
@Getter
@ConfigurationProperties(prefix = Alerter.KEY_PREFIX + ".alerter")
public class AlerterProperties extends ToJsonString implements Serializable {
    private static final long serialVersionUID = 3369292434171863341L;

    /**
     * Type channels mapping, Map[AlertType, channel[]]
     */
    private Map<AlertType, String[]> typeChannelsMap = new EnumMap<>(AlertType.class);

    /**
     * Send thread pool config
     */
    private SendThreadPool sendThreadPool = new SendThreadPool();

    /**
     * Send rate limit config
     */
    private SendRateLimit sendRateLimit = new SendRateLimit();

    @Getter
    @Setter
    public static class SendThreadPool extends ToJsonString {
        private int corePoolSize = 2;
        private int maximumPoolSize = 8;
        private int queueCapacity = 100;
        private int keepAliveTimeSeconds = 300;
        private boolean allowCoreThreadTimeOut = true;
        private int awaitTerminationSeconds = 3;
    }

    @Getter
    @Setter
    public static class SendRateLimit extends ToJsonString {
        private int maxRequests = 20;
        private long windowSizeInMillis = 1000;
    }

}

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

package cn.ponfee.disjob.alert.sms.configuration;

import cn.ponfee.disjob.alert.Alerter;
import cn.ponfee.disjob.alert.sender.AlertSenderProperties;
import cn.ponfee.disjob.alert.sms.SmsAlertSender;
import cn.ponfee.disjob.common.base.ToJsonString;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Sms alert sender properties
 *
 * @author TJxiaobao
 */
@Getter
@Setter
@ConfigurationProperties(prefix = Alerter.SENDER_CONFIG_KEY_PREFIX + "." + SmsAlertSender.CHANNEL)
public class SmsAlertSenderProperties extends AlertSenderProperties {
    private static final long serialVersionUID = 2531779048449076379L;

    private Map<String, SmsBlendProperties> blends = new HashMap<>();

    @Getter
    @Setter
    public static class SmsBlendProperties extends ToJsonString implements Serializable {
        private static final long serialVersionUID = 1305124631930608648L;

        private String accessKeyId;
        private String accessKeySecret;
        private String signature;
        private String templateId;
        private String supplier;
    }

}

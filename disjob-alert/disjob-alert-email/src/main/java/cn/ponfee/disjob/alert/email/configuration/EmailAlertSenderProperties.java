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

package cn.ponfee.disjob.alert.email.configuration;

import cn.ponfee.disjob.alert.email.EmailAlertSender;
import cn.ponfee.disjob.alert.sender.AlertSenderProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Email alert properties
 *
 * @author Ponfee
 */
@Getter
@Setter
@ConfigurationProperties(prefix = AlertSenderProperties.KEY_PREFIX + "." + EmailAlertSender.CHANNEL)
public class EmailAlertSenderProperties extends AlertSenderProperties {

    private static final long serialVersionUID = 2531779048449076379L;

    private String host;
    private String protocol;
    private String username;
    private String password;

}

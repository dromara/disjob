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

package cn.ponfee.disjob.alert.im.configuration;

import cn.ponfee.disjob.alert.Alerter;
import cn.ponfee.disjob.alert.im.ImAlertSender;
import cn.ponfee.disjob.alert.im.ImAlertSupplier;
import cn.ponfee.disjob.alert.sender.AlertSenderProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Instant messaging alert sender properties
 *
 * @author Ponfee
 */
@Getter
@Setter
@ConfigurationProperties(prefix = Alerter.SENDER_CONFIG_KEY_PREFIX + "." + ImAlertSender.CHANNEL)
public class ImAlertSenderProperties extends AlertSenderProperties {
    private static final long serialVersionUID = 2531779048449076379L;

    /**
     * Token id
     */
    private String tokenId;

    /**
     * Secret key
     */
    private String secretKey;

    /**
     * Supplier
     */
    private ImAlertSupplier supplier;

}

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
import cn.ponfee.disjob.alert.sender.UserRecipientMapper;
import cn.ponfee.disjob.alert.sms.SmsAlertReadConfig;
import cn.ponfee.disjob.alert.sms.SmsAlertSender;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * SmsAlertSender auto configuration
 *
 * @author TJxiaobao
 */
@ConditionalOnProperty(name = Alerter.ENABLED_KEY, havingValue = "true", matchIfMissing = true)
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@EnableConfigurationProperties(SmsAlertSenderProperties.class)
public class SmsAlertSenderAutoConfiguration {

    public static final String SMS_USER_RECIPIENT_MAPPER_BEAN_NAME = Alerter.USER_RECIPIENT_MAPPER_BEAN_NAME_PREFIX + "." + SmsAlertSender.CHANNEL;

    @ConditionalOnMissingBean(name = SMS_USER_RECIPIENT_MAPPER_BEAN_NAME)
    @Bean(SMS_USER_RECIPIENT_MAPPER_BEAN_NAME)
    public UserRecipientMapper smsUserRecipientMapper() {
        return new UserRecipientMapper();
    }

    @ConditionalOnMissingBean
    @Bean
    public SmsAlertReadConfig smsAlertReadConfig(SmsAlertSenderProperties config) {
        return new SmsAlertReadConfig(config);
    }

    @Bean
    public SmsAlertSender smsAlertSender(@Qualifier(SMS_USER_RECIPIENT_MAPPER_BEAN_NAME) UserRecipientMapper mapper) {
        return new SmsAlertSender(mapper);
    }

}

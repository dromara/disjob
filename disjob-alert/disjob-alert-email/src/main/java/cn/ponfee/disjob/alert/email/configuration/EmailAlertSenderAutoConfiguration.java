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

import cn.ponfee.disjob.alert.Alerter;
import cn.ponfee.disjob.alert.email.EmailAlertSender;
import cn.ponfee.disjob.alert.sender.UserRecipientMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * EmailAlertSender auto configuration
 *
 * @author Ponfee
 */
@ConditionalOnExpression(Alerter.ENABLED_KEY_EXPRESSION)
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@EnableConfigurationProperties(EmailAlertSenderProperties.class)
public class EmailAlertSenderAutoConfiguration {

    public static final String EMAIL_USER_RECIPIENT_MAPPER_BEAN_NAME = Alerter.USER_RECIPIENT_MAPPER_BEAN_NAME_PREFIX + "." + EmailAlertSender.CHANNEL;

    @ConditionalOnMissingBean(name = EMAIL_USER_RECIPIENT_MAPPER_BEAN_NAME)
    @Bean(EMAIL_USER_RECIPIENT_MAPPER_BEAN_NAME)
    public UserRecipientMapper emailUserRecipientMapper() {
        return new UserRecipientMapper();
    }

    @Bean
    public EmailAlertSender emailAlertSender(EmailAlertSenderProperties config,
                                             @Qualifier(EMAIL_USER_RECIPIENT_MAPPER_BEAN_NAME) UserRecipientMapper mapper) {
        return new EmailAlertSender(config, mapper);
    }

}

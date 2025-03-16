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

import cn.ponfee.disjob.alert.sms.SmsAlertSender;
import cn.ponfee.disjob.alert.sms.SmsUserRecipientMapper;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotationMetadata;

/**
 * SmsAlertSender auto configuration
 *
 * @author TJxiaobao
 */
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@EnableConfigurationProperties(SmsAlertSenderProperties.class)
@Import(SmsAlertSenderAutoConfiguration.SmsAlertSenderDeferredImportSelector.class)
public class SmsAlertSenderAutoConfiguration {

    static class SmsAlertSenderDeferredImportSelector implements DeferredImportSelector {
        @SuppressWarnings("NullableProblems")
        @Override
        public String[] selectImports(AnnotationMetadata importingClassMetadata) {
            return new String[]{SmsAlertSenderDeferredConfiguration.class.getName()};
        }
    }

    static class SmsAlertSenderDeferredConfiguration {
        @ConditionalOnMissingBean
        @Bean
        public SmsUserRecipientMapper smsUserRecipientMapper() {
            return new SmsUserRecipientMapper();
        }
    }

    @Bean
    public SmsAlertSender smsAlertSender(SmsAlertSenderProperties config, SmsUserRecipientMapper mapper) {
        return new SmsAlertSender(config, mapper);
    }
}

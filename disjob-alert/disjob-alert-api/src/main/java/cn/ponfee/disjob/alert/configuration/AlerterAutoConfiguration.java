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
import cn.ponfee.disjob.core.base.GroupInfoService;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Alerter auto configuration
 *
 * @author Ponfee
 */
@ConditionalOnExpression(Alerter.ENABLED_KEY_EXPRESSION)
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@EnableConfigurationProperties(AlerterProperties.class)
public class AlerterAutoConfiguration {

    @Bean
    public Alerter alerter(AlerterProperties alerterConfig, GroupInfoService groupInfoService) {
        return new Alerter(alerterConfig, groupInfoService);
    }

}

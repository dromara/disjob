package cn.ponfee.disjob.alert;

import cn.ponfee.disjob.alert.base.AlerterProperties;
import cn.ponfee.disjob.alert.sender.AlertSender;
import cn.ponfee.disjob.core.supervisor.GroupInfoService;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Alerter auto configuration
 *
 * @author Ponfee
 */
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnExpression(Alerter.ENABLED_KEY_EXPRESSION)
@ConditionalOnBean(AlertSender.class)
@EnableConfigurationProperties(AlerterProperties.class)
public class AlerterConfiguration {

    @ConditionalOnMissingBean
    @Bean
    Alerter alerter(AlerterProperties alerterConfig, GroupInfoService groupInfoService) {
        return new Alerter(alerterConfig, groupInfoService);
    }

}

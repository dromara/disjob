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


package cn.ponfee.disjob.alert.sms;

import cn.ponfee.disjob.alert.event.AlertEvent;
import cn.ponfee.disjob.alert.sender.AlertSender;
import cn.ponfee.disjob.alert.sms.configuration.SmsAlertSenderProperties;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Sms alert sender
 *
 * @author TJxiaobao
 */
public class SmsAlertSender extends AlertSender {

    public static final String CHANNEL = "sms";

    public SmsAlertSenderProperties config;

    private static final Logger LOG = LoggerFactory.getLogger(SmsAlertSender.class);

    public SmsAlertSender(SmsAlertSenderProperties config, SmsUserRecipientMapper mapper) {
        super(CHANNEL, "短信", mapper);
        // todo: init sms client by config
        this.config = config;
    }

    @Override
    protected void doSend(AlertEvent alertEvent, Map<String, String> alertRecipients, String webhook) {
        SmsBlend smsBlend = SmsFactory.getSmsBlend();
        if (alertRecipients == null || alertRecipients.isEmpty()) {
            LOG.warn("No recipients found for alert event: {}", alertEvent);
            return;
        }
        for (Map.Entry<String, String> entry : alertRecipients.entrySet()) {
            String alertUser = entry.getKey();
            if (alertUser == null || (alertUser.contains("@") && alertUser.contains(".com"))) {
                LOG.warn("Sms invalid recipient found for alert event: {}", alertEvent);
                continue;
            }
            String recipientPhoneNumber = entry.getValue();
            try {
                String message = buildMessage(alertEvent);
                smsBlend.sendMessage(recipientPhoneNumber, message);
                LOG.info("Alert sent to {} ({}) for event: {}", alertUser, recipientPhoneNumber, alertEvent);
            } catch (Exception e) {
                LOG.error("Failed to send alert to {} ({}) for event: {}", alertUser, recipientPhoneNumber, alertEvent, e);
            }
        }
    }

    private String buildMessage(AlertEvent alertEvent) {
        return String.format(
            "Alert Notification\n\nAlert Type: %s\nTimestamp: %s\n\nDetails:\n%s",
            alertEvent.getAlertType(),
            alertEvent.buildTitle(),
            alertEvent.buildContent()
        );
    }
}

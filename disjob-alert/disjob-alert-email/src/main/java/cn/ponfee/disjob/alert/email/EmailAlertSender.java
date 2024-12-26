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

package cn.ponfee.disjob.alert.email;

import cn.ponfee.disjob.alert.email.configuration.EmailAlertSenderProperties;
import cn.ponfee.disjob.alert.event.AlertEvent;
import cn.ponfee.disjob.alert.sender.AlertSender;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;

/**
 * Email alert sender
 *
 * @author Ponfee
 */
public class EmailAlertSender extends AlertSender {

    private static final Logger LOG = LoggerFactory.getLogger(EmailAlertSender.class);
    public static final String CHANNEL = "email";

    private final Session mailSession;
    private final EmailAlertSenderProperties emailConfig;

    public EmailAlertSender(EmailAlertSenderProperties config) {
        super(CHANNEL, "邮件");
        this.emailConfig = config;
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", config.isStartTlsEnabled());
        props.put("mail.smtp.host", config.getHost());
        props.put("mail.smtp.port", config.getPort());
        props.put("mail.smtp.ssl.enable", config.isSslEnabled());

        this.mailSession = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.getUsername(), config.getPassword());
            }
        });

        LOG.info("EmailAlertSender initialized with host: {}, port: {}, startTLS: {}, SSL: {}",
            config.getHost(), config.getPort(), config.isStartTlsEnabled(), config.isSslEnabled());

    }

    @Override
    protected void doSend(AlertEvent alertEvent, Map<String, String> alertRecipients, String webhook) {
        if (alertRecipients == null || alertRecipients.isEmpty()) {
            LOG.warn("No recipients found for alert event: {}", alertEvent);
            return;
        }
        for (Map.Entry<String, String> recipient : alertRecipients.entrySet()) {
            String recipientEmail = recipient.getKey();
            String recipientName = recipient.getValue();
            try {
                Message message = new MimeMessage(mailSession);
                message.setFrom(new InternetAddress(emailConfig.getFromAddress())); // Sender
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail)); // Recipient
                Transport.send(message);

                LOG.info("Alert email sent to {} ({}) for event: {}", recipientEmail, recipientName, alertEvent);
            } catch (MessagingException e) {
                LOG.error("Failed to send alert email to {} ({}) for event: {}", recipientEmail, recipientName, alertEvent, e);
            }
        }
    }

}

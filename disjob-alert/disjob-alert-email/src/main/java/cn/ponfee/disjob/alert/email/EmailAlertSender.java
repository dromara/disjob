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
import cn.ponfee.disjob.alert.sender.AlertSender;
import cn.ponfee.disjob.alert.sender.UserRecipientMapper;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.core.alert.AlertEvent;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.NamingException;
import java.util.Date;
import java.util.Map;

/**
 * Email alert sender
 *
 * @author Ponfee
 */
public class EmailAlertSender extends AlertSender {

    public static final String CHANNEL = "email";
    private static final Logger LOG = LoggerFactory.getLogger(EmailAlertSender.class);

    private final EmailAlertSenderProperties config;
    private final JavaMailSenderImpl sender;

    public EmailAlertSender(EmailAlertSenderProperties config, UserRecipientMapper mapper) {
        super(CHANNEL, "Email", mapper);
        this.config = config;
        this.sender = createMailSender(config);
        LOG.info("Email alert sender initialized: {}", config);
    }

    @Override
    protected void doSend(AlertEvent alertEvent, Map<String, String> alertRecipients, String webhook) {
        if (MapUtils.isEmpty(alertRecipients)) {
            LOG.warn("Alert email recipients is empty.");
            return;
        }
        try {
            MimeMessage mimeMessage = sender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
            mimeMessageHelper.setFrom(config.getUsername());
            mimeMessageHelper.setTo(buildRecipients(alertRecipients));
            mimeMessageHelper.setSubject(alertEvent.buildTitle());
            mimeMessageHelper.setText(alertEvent.buildContent("<b>%s</b>%s<br/>"), true);
            mimeMessageHelper.setSentDate(new Date());
            sender.send(mimeMessage);
            LOG.info("Alert event email send success: {}", alertRecipients.values());
        } catch (Exception e) {
            LOG.error("Alert event email send error: " + alertRecipients.values(), e);
        }
    }

    // ----------------------------------------------------------private static methods

    private static JavaMailSenderImpl createMailSender(EmailAlertSenderProperties config) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        String jndiName = config.getJndiName();
        if (StringUtils.isNotBlank(jndiName)) {
            try {
                sender.setSession(JndiLocatorDelegate.createDefaultResourceRefLocator().lookup(jndiName, Session.class));
            } catch (NamingException ex) {
                throw new IllegalStateException("Alert email sender jndi name unable.", ex);
            }
        } else {
            sender.setHost(config.getHost());
            if (config.getPort() != null) {
                sender.setPort(config.getPort());
            }
            sender.setUsername(config.getUsername());
            sender.setPassword(config.getPassword());
            sender.setProtocol(config.getProtocol());
            if (!config.getProperties().isEmpty()) {
                sender.setJavaMailProperties(Collects.toProperties(config.getProperties()));
            }
        }

        if (config.getDefaultEncoding() != null) {
            sender.setDefaultEncoding(config.getDefaultEncoding().name());
        }

        // testing connection
        if (config.isTestConnection()) {
            try {
                sender.testConnection();
            } catch (MessagingException ex) {
                throw new IllegalStateException("Alert email sender server unavailable.", ex);
            }
        }
        return sender;
    }

    private static InternetAddress[] buildRecipients(Map<String, String> alertRecipients) throws Exception {
        InternetAddress[] recipients = new InternetAddress[alertRecipients.size()];
        int i = 0;
        for (Map.Entry<String, String> e : alertRecipients.entrySet()) {
            recipients[i++] = new InternetAddress(e.getValue(), e.getKey());
        }
        return recipients;
    }

}

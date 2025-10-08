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
import cn.ponfee.disjob.alert.sender.AlertSenderProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Email alert sender properties
 *
 * @author Ponfee
 * @see org.springframework.boot.autoconfigure.mail.MailProperties
 */
@Getter
@Setter
@ConfigurationProperties(prefix = Alerter.SENDER_CONFIG_KEY_PREFIX + "." + EmailAlertSender.CHANNEL)
public class EmailAlertSenderProperties extends AlertSenderProperties {
    private static final long serialVersionUID = 2531779048449076379L;

    /**
     * SMTP server host. For instance, 'smtp.example.com'.
     */
    private String host;

    /**
     * SMTP server port.
     */
    private Integer port;

    /**
     * Login user of the SMTP server.
     */
    private String username;

    /**
     * Login password of the SMTP server.
     */
    private String password;

    /**
     * Protocol used by the SMTP server.
     */
    private String protocol = "smtp";

    /**
     * Default MimeMessage encoding.
     */
    private Charset defaultEncoding = StandardCharsets.UTF_8;

    /**
     * Additional JavaMail Session properties.
     */
    private Map<String, String> properties = new HashMap<>();

    /**
     * SSL configuration.
     */
    private final Ssl ssl = new Ssl();

    /**
     * Session JNDI name. When set, takes precedence over other Session settings.
     */
    private String jndiName;

    /**
     * Testing email service connectivity on startup.
     */
    private boolean testConnection = false;

    @Getter
    @Setter
    public static class Ssl {
        /**
         * Whether to enable SSL support. If enabled, 'mail.(protocol).ssl.enable'
         * property is set to 'true'.
         */
        private boolean enabled = false;

        /**
         * SSL bundle name. If set, 'mail.(protocol).ssl.socketFactory' property is set to
         * an SSLSocketFactory obtained from the corresponding SSL bundle.
         * <p>
         * Note that the STARTTLS command can use the corresponding SSLSocketFactory, even
         * if the 'mail.(protocol).ssl.enable' property is not set.
         */
        private String bundle;
    }

}

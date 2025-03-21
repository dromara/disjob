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

package cn.ponfee.disjob.alert.sender;

import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.core.alert.AlertEvent;
import lombok.Getter;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.util.*;

/**
 * Abstract alert sender
 *
 * @author Ponfee
 */
@Getter
public abstract class AlertSender extends SingletonClassConstraint {

    /**
     * Registered all alert senders, Map[channel, AlertSender]
     */
    private static final Map<String, AlertSender> ALERT_SENDERS = new HashMap<>();

    /**
     * 渠道类型
     */
    private final String channel;

    /**
     * 渠道名称
     */
    private final String name;

    /**
     * 把在页面中配置的alertUsers转为实际的渠道收件人（如邮箱地址、钉钉号、企业微信号、飞书账号、手机号等）
     */
    private final UserRecipientMapper userRecipientMapper;

    protected AlertSender(String channel, String name, UserRecipientMapper mapper) {
        Assert.hasText(channel, "Alert sender channel cannot be blank.");
        Assert.hasText(name, "Alert sender name cannot be blank.");
        this.channel = channel.trim();
        this.name = name.trim();
        this.userRecipientMapper = Objects.requireNonNull(mapper);
        register(this);
    }

    public void send(AlertEvent alertEvent, Set<String> alertUsers, String webhook) {
        Map<String, String> recipients = userRecipientMapper.mapping(alertUsers);
        if (verify(recipients, webhook)) {
            doSend(alertEvent, recipients, webhook);
        }
    }

    protected boolean verify(Map<String, String> recipients, String webhook) {
        return MapUtils.isNotEmpty(recipients) || StringUtils.isNotBlank(webhook);
    }

    /**
     * Do send alert event message.
     *
     * @param alertEvent      the alert event
     * @param alertRecipients the alert recipients [user -> recipient]
     * @param webhook         the webhook
     */
    protected abstract void doSend(AlertEvent alertEvent, Map<String, String> alertRecipients, String webhook);

    // ------------------------------------------------------------------static methods

    public static List<AlertSender> all() {
        return new ArrayList<>(ALERT_SENDERS.values());
    }

    public static AlertSender get(String channel) {
        return ALERT_SENDERS.get(channel);
    }

    private static synchronized void register(AlertSender alertSender) {
        String channel = alertSender.channel;
        if (ALERT_SENDERS.containsKey(channel)) {
            throw new Error("Alert sender channel '" + channel + "' already registered!");
        }
        ALERT_SENDERS.put(channel, alertSender);
    }

}

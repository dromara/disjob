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

import cn.ponfee.disjob.alert.base.AlertEvent;
import cn.ponfee.disjob.common.base.SingletonClassConstraint;
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
     * 把在页面中配置的alertRecipients转为实际的渠道接收人（如邮箱地址、钉钉号、企业微信号、飞书账号、手机号等）
     */
    private final AlertRecipientMapper alertRecipientMapper;

    protected AlertSender(String channel, String name, AlertRecipientMapper mapper) {
        Assert.hasText(channel, "Alert sender channel cannot be blank.");
        Assert.hasText(name, "Alert sender name cannot be blank.");
        this.channel = channel.trim();
        this.name = name.trim();
        this.alertRecipientMapper = Objects.requireNonNull(mapper);
        register(this);
    }

    public final void send(AlertEvent alertEvent, Set<String> alertRecipients, String alertWebhook) {
        Map<String, String> alertRecipientMap = alertRecipientMapper.mapping(alertRecipients);
        if (verify(alertRecipientMap, alertWebhook)) {
            send(alertEvent, alertRecipientMap, alertWebhook);
        }
    }

    /**
     * Verifies the alert recipient map and webhook url param
     *
     * @param alertRecipientMap the alert recipient map
     * @param alertWebhook      the alert webhook url
     * @return {@code true} is verified success
     */
    protected boolean verify(Map<String, String> alertRecipientMap, String alertWebhook) {
        return MapUtils.isNotEmpty(alertRecipientMap) || StringUtils.isNotBlank(alertWebhook);
    }

    /**
     * Sends the alert event by current message channel.
     *
     * @param alertEvent        the alert event
     * @param alertRecipientMap the alert recipient map[origin-recipient -> channel-recipient]
     * @param alertWebhook      the alert webhook url
     */
    protected abstract void send(AlertEvent alertEvent, Map<String, String> alertRecipientMap, String alertWebhook);

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

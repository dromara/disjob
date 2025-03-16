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

import cn.ponfee.disjob.alert.event.AlertEvent;
import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Abstract alert sender
 *
 * @author Ponfee
 */
@Getter
public abstract class AlertSender extends SingletonClassConstraint {

    private static final Map<String, AlertSender> ALERT_SENDERS = new HashMap<>();

    private final String channel;
    private final String name;

    protected AlertSender(String channel, String name) {
        this.channel = channel;
        this.name = name;
        registerAlertSender(this);
    }

    public void send(AlertEvent alertEvent, Set<String> alertUsers, String webhook) {
        doSend(alertEvent, mapRecipients(alertUsers), webhook);
    }

    public void checkAlertUsers(Set<String> alertUsers) {
        if (CollectionUtils.isEmpty(alertUsers)) {
            return;
        }
        Map<String, String> alertRecipients = mapRecipients(alertUsers);
        List<String> list = alertUsers.stream()
            .filter(e -> StringUtils.isBlank(alertRecipients.get(e)))
            .collect(Collectors.toList());
        Assert.isTrue(list.isEmpty(), () -> "Invalid alert users: " + list);
    }

    public Map<String, String> mapRecipients(Set<String> alertUsers) {
        if (CollectionUtils.isEmpty(alertUsers)) {
            return Collections.emptyMap();
        }
        return alertUsers.stream().collect(Collectors.toMap(Function.identity(), Function.identity()));
    }

    /**
     * Do send alert message.
     *
     * @param alertEvent      the alert event
     * @param alertRecipients the alert recipients
     * @param webhook         the webhook
     */
    protected abstract void doSend(AlertEvent alertEvent, Map<String, String> alertRecipients, String webhook);

    // ------------------------------------------------------------------static methods

    public static List<AlertSender> getAllAlertSenders() {
        return new ArrayList<>(ALERT_SENDERS.values());
    }

    public static AlertSender getAlertSender(String channel) {
        return ALERT_SENDERS.get(channel);
    }

    private static synchronized void registerAlertSender(AlertSender alertSender) {
        String channel = alertSender.channel;
        if (ALERT_SENDERS.containsKey(channel)) {
            throw new Error("Channel '" + channel + "' already registered!");
        }
        ALERT_SENDERS.put(channel, alertSender);
    }

}

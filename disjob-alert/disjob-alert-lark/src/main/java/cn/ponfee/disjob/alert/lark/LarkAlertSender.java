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

package cn.ponfee.disjob.alert.lark;

import cn.ponfee.disjob.alert.event.AlertEvent;
import cn.ponfee.disjob.alert.lark.configuration.LarkAlertSenderProperties;
import cn.ponfee.disjob.alert.sender.AlertSender;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.springframework.core.NestedExceptionUtils.buildMessage;

/**
 * Lark alert sender
 *
 * @author Ponfee
 */
public class LarkAlertSender extends AlertSender {

    public static final String CHANNEL = "lark";

    private static final Logger LOG = LoggerFactory.getLogger(LarkAlertSender.class);

    private  LarkAlertSenderProperties config;

    public LarkAlertSender(LarkAlertSenderProperties config, LarkUserRecipientMapper mapper) {
        super(CHANNEL, "飞书", mapper);
        this.config = config;

    }


    @Override
    protected void doSend(AlertEvent alertEvent, Map<String, String> alertRecipients, String webhook) {
        if (alertRecipients == null || alertRecipients.isEmpty()) {
            LOG.warn("No recipients found for alert event: {}", alertEvent);
            return;
        }

        for (Map.Entry<String, String> entry : alertRecipients.entrySet()) {
            String alertUser = entry.getKey();
            String recipient = entry.getValue();
            try {
                String message = buildMessage(alertEvent);
                if ("we_talk".equals(config.getSupplier())) {
                    // 企业微信不需要 sign
                    webhook = String.format("%s?token=%s", webhook, config.getTokenId());
                } else {
                    // lark and dingtalk
                    webhook = String.format("%s?token=%s&sign=%s", webhook, config.getTokenId(), config.getSign());
                }
                sendHttpPostRequest(webhook, message, config.getSupplier());
                LOG.info("Alert sent to {} ({}) for event: {}", alertUser, recipient, alertEvent);
            } catch (Exception e) {
                LOG.error("Failed to send alert to {} ({}) for event: {}", alertUser, recipient, alertEvent, e);
            }
        }
    }

    private void sendHttpPostRequest(String webhookUrl, String message, String supplier) throws Exception {
        URL url = new URL(webhookUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        String jsonPayload = createJsonPayload(message, supplier);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonPayload.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            System.out.println("Message sent successfully!");
        } else {
            System.out.println("Failed to send message. Response code: " + responseCode);
        }
    }

    private String createJsonPayload(String message, String supplier) {
        // 根据不同的OA来生成 JSON 负载
        switch (supplier) {
            case "ding_talk":
                return "{\"msgtype\": \"text\", \"text\": {\"content\": \"" + message + "\"}}";
            case "we_talk":
                return "{\"text\": {\"content\": \"" + message + "\"}}";
            case "lark":
                return "{\"msg_type\": \"text\", \"content\": {\"text\": \"" + message + "\"}}";
            default:
                throw new IllegalArgumentException("Unsupported supplier: " + supplier);
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

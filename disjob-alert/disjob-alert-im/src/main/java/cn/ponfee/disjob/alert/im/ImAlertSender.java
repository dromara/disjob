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

package cn.ponfee.disjob.alert.im;

import cn.ponfee.disjob.alert.event.AlertEvent;
import cn.ponfee.disjob.alert.im.configuration.ImAlertSenderProperties;
import cn.ponfee.disjob.alert.sender.AlertSender;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * IM alert sender
 *
 * @author Ponfee
 */
@Slf4j
public class ImAlertSender extends AlertSender {

    public static final String CHANNEL = "im";

    private static final Logger LOG = LoggerFactory.getLogger(ImAlertSender.class);

    private final ImAlertSenderProperties config;

    public ImAlertSender(ImAlertSenderProperties config, ImUserRecipientMapper mapper) {
        super(CHANNEL, "Instant Messaging", mapper);
        this.config = config;
    }


    @Override
    protected void doSend(AlertEvent alertEvent, Map<String, String> alertRecipients, String webhook) {
        if (alertRecipients == null || alertRecipients.isEmpty()) {
            LOG.warn("No recipients found for alert event: {}", alertEvent);
            return;
        }

        String title = alertEvent.buildTitle();
        String message = alertEvent.buildContent();
        if (message == null || message.trim().isEmpty()) {
            LOG.warn("Empty message content for alert event: {}", alertEvent);
            return;
        }

        // 对消息内容进行转义，避免JSON格式问题
        message = message.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n");

        for (Map.Entry<String, String> entry : alertRecipients.entrySet()) {
            String alertUser = entry.getKey();
            String recipient = entry.getValue();
            try {
                sendHttpPostRequest(webhook, title, message, config.getSupplier());
                LOG.info("Alert sent to {} ({}) for event: {}", alertUser, recipient, alertEvent);
            } catch (Exception e) {
                LOG.error("Failed to send alert to {} ({}) for event: {}", alertUser, recipient, alertEvent, e);
            }
        }
    }

    private static String genLarkSign(String secret, int timestamp) throws NoSuchAlgorithmException, InvalidKeyException {
        //把timestamp+"\n"+密钥当做签名字符串
        String stringToSign = timestamp + "\n" + secret;

        //使用HmacSHA256算法计算签名
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(new byte[]{});
        return new String(Base64.encodeBase64(signData));
    }

    private void sendHttpPostRequest(String webhookUrl, String title, String message, String supplier) throws Exception {
        URL url = new URL(webhookUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        String jsonPayload = createJsonPayload(title, message, supplier);
        log.info("Sending request - URL: {}, Supplier: {}, Payload: {}", webhookUrl, supplier, jsonPayload);
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        int responseCode = connection.getResponseCode();
        String responseBody;
        // 根据响应码选择正确的流
        try (InputStream inputStream = responseCode >= 400
            ? connection.getErrorStream()
            : connection.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            responseBody = response.toString();
        }
        if (responseCode == HttpURLConnection.HTTP_OK) {
            log.info("Message sent successfully! Response: {}", responseBody);
        } else {
            log.error("Failed to send message. Response code: {}, Response body: {}", responseCode, responseBody);
        }
    }

    private String createJsonPayload(String title, String message, String supplier) {
        // 根据不同的OA来生成 JSON 负载
        switch (supplier) {
            case "ding_talk":
                return "{\"msgtype\": \"text\", \"text\": {\"content\": \"" + message + "\"}}";
            case "we_talk":
                return "{\"text\": {\"content\": \"" + message + "\"}}";
            case "lark":
                int currentTimeMillis = (int) (System.currentTimeMillis() / 1000);
                try {
                    return createImJsonPayload(currentTimeMillis, genLarkSign(config.getSign(), currentTimeMillis), title, message);
                } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                    throw new RuntimeException(e);
                }
            default:
                throw new IllegalArgumentException("Unsupported supplier: " + supplier);
        }
    }

    private String createImJsonPayload(long timestamp, String sign, String title, String message) {
        return String.format(
            "{\"timestamp\": %d," +
                "\"sign\": \"%s\"," +
                "\"msg_type\": \"post\"," +
                "\"content\": {" +
                "\"post\": {" +
                "\"zh_cn\": {" +
                "\"title\": \"%s\"," +
                "\"content\": [[{\"tag\":\"text\",\"text\":\"%s\"}]]" +
                "}" +
                "}" +
                "}}",
            timestamp, sign, title, message
        );
    }

}

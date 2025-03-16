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
import cn.ponfee.disjob.alert.sender.UserRecipientMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Instant messaging alert sender
 *
 * @author Ponfee
 */
@Slf4j
public class ImAlertSender extends AlertSender {

    public static final String CHANNEL = "im";
    private static final Logger LOG = LoggerFactory.getLogger(ImAlertSender.class);

    private final ImAlertSenderProperties config;

    public ImAlertSender(ImAlertSenderProperties config, UserRecipientMapper mapper) {
        super(CHANNEL, "Instant Messaging", mapper);
        this.config = config;
        LOG.info("Instant Messaging alert sender initialized: {}", config);
    }

    @Override
    protected void doSend(AlertEvent alertEvent, Map<String, String> alertRecipients, String webhook) {
        if (StringUtils.isBlank(webhook)) {
            LOG.warn("Alert instant messaging webhook is empty.");
            return;
        }

        String title = alertEvent.buildTitle();
        String content = alertEvent.buildContent("", "\n");
        String message = StringUtils.replaceEach(content, new String[]{"\\", "\"", "\n"}, new String[]{"\\\\", "\\\"", "\\n"});
        ImAlertSupplier supplier = config.getSupplier();
        try {
            String payload = buildPayload(title, message, supplier);
            doPost(webhook, payload);
            LOG.info("Alert event instant messaging sent success: {}, {}", supplier, webhook);
        } catch (Exception e) {
            LOG.error("Alert event instant messaging sent error: " + supplier + ", " + webhook, e);
        }
    }

    // -----------------------------------------------------------private methods

    private void doPost(String webhook, String payload) throws Exception {
        URL url = new URL(webhook);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        log.info("Alert instant messaging webhook request: {}, {}", webhook, payload);
        try (OutputStream writeStream = connection.getOutputStream()) {
            writeStream.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = connection.getResponseCode();
        // http status code: 1xx, 2xx, 3xx
        boolean isSuccess = responseCode < 400;
        try (InputStream readStream = isSuccess ? connection.getInputStream() : connection.getErrorStream()) {
            String responseBody = IOUtils.toString(readStream, StandardCharsets.UTF_8);
            if (isSuccess) {
                log.info("Alert instant messaging webhook success: {}, {}", responseCode, responseBody);
            } else {
                log.error("Alert instant messaging webhook failed: {}, {}", responseCode, responseBody);
            }
        }
    }

    private String buildPayload(String title, String message, ImAlertSupplier supplier) {
        switch (supplier) {
            case DING_TALK:
                return "{\"msgtype\": \"text\", \"text\": {\"content\": \"" + message + "\"}}";
            case WE_COM:
                return "{\"text\": {\"content\": \"" + message + "\"}}";
            case LARK:
                long timestamp = System.currentTimeMillis() / 1000;
                String secretKey = timestamp + "\n" + config.getSecretKey();
                HmacUtils hmacUtils = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secretKey);
                String signature = Base64.getEncoder().encodeToString(hmacUtils.hmac(ArrayUtils.EMPTY_BYTE_ARRAY));
                String format = "{\"timestamp\":%d,\"sign\":\"%s\",\"msg_type\":\"post\",\"content\":{\"post\":{\"zh_cn\":{\"title\":\"%s\",\"content\":[[{\"tag\":\"text\",\"text\":\"%s\"}]]}}}}";
                return String.format(format, timestamp, signature, title, message);
            default:
                throw new UnsupportedOperationException("Unknown instant messaging supplier: " + supplier);
        }
    }

}

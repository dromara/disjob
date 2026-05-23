/*
 * Copyright 2022-2026 Ponfee (http://www.ponfee.cn/)
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

import cn.ponfee.disjob.alert.base.AlertEvent;
import cn.ponfee.disjob.alert.im.configuration.ImAlertSenderProperties;
import cn.ponfee.disjob.alert.sender.AlertRecipientMapper;
import cn.ponfee.disjob.alert.sender.AlertSender;
import cn.ponfee.disjob.common.util.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

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

    private final ImAlertSenderProperties config;

    public ImAlertSender(ImAlertSenderProperties config, AlertRecipientMapper mapper) {
        super(CHANNEL, "Instant Messaging", mapper);
        this.config = config;
        log.info("Instant Messaging alert sender initialized: {}", config);
    }

    @Override
    protected void send(AlertEvent alertEvent, Map<String, String> alertRecipientMap, String alertWebhook) {
        if (StringUtils.isBlank(alertWebhook)) {
            log.warn("Alert instant messaging webhook is empty.");
            return;
        }

        ImAlertSupplier supplier = config.getSupplier();
        HttpURLConnection connection = null;
        OutputStream writeStream = null;
        InputStream readStream = null;
        try {
            String title = alertEvent.buildTitle();
            String content = alertEvent.buildContent("**%s**%s\n");
            String message = StringUtils.replaceEach(content, new String[]{"\\", "\"", "\n"}, new String[]{"\\\\", "\\\"", "\\n"});
            String payload = buildPayload(title, message, supplier);

            connection = (HttpURLConnection) new URL(alertWebhook).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            log.info("Alert instant messaging webhook request: {}, {}", alertWebhook, payload);
            writeStream = connection.getOutputStream();
            writeStream.write(payload.getBytes(StandardCharsets.UTF_8));

            int responseCode = connection.getResponseCode();
            // http status code: 1xx, 2xx, 3xx
            boolean isSuccess = responseCode < 400;
            readStream = isSuccess ? connection.getInputStream() : connection.getErrorStream();
            String responseBody = IOUtils.toString(readStream, StandardCharsets.UTF_8);
            if (isSuccess) {
                log.info("Alert instant messaging webhook success: {}, {}", responseCode, responseBody);
            } else {
                log.error("Alert instant messaging webhook failed: {}, {}", responseCode, responseBody);
            }
            log.info("Alert event instant messaging sent success: {}, {}", supplier, alertWebhook);
        } catch (Exception e) {
            log.error("Alert event instant messaging sent error: {}, {}", supplier, alertWebhook, e);
        } finally {
            IOUtils.closeQuietly(readStream);
            IOUtils.closeQuietly(writeStream);
            ObjectUtils.applyIfNotNull(connection, HttpURLConnection::disconnect);
        }
    }

    // -----------------------------------------------------------private methods

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

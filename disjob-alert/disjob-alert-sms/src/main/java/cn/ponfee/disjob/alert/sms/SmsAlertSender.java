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


package cn.ponfee.disjob.alert.sms;

import cn.ponfee.disjob.alert.event.AlertEvent;
import cn.ponfee.disjob.alert.sender.AlertSender;
import cn.ponfee.disjob.alert.sender.UserRecipientMapper;
import org.apache.commons.collections4.MapUtils;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sms alert sender
 *
 * @author TJxiaobao
 */
public class SmsAlertSender extends AlertSender {

    public static final String CHANNEL = "sms";
    private static final Logger LOG = LoggerFactory.getLogger(SmsAlertSender.class);

    public SmsAlertSender(UserRecipientMapper mapper) {
        super(CHANNEL, "Short Message Service", mapper);
        LOG.info("SMS alert sender initialized.");
    }

    @Override
    protected void doSend(AlertEvent alertEvent, Map<String, String> alertRecipients, String webhook) {
        if (MapUtils.isEmpty(alertRecipients)) {
            LOG.warn("Alert sms phones is empty.");
            return;
        }

        SmsBlend smsBlend = SmsFactory.getSmsBlend();
        String message = alertEvent.buildTitle() + "\n" + alertEvent.buildContent("\t", "\n");
        List<String> phones = alertRecipients.values().stream().distinct().collect(Collectors.toList());
        try {
            smsBlend.massTexting(phones, message);
            LOG.info("Alert event sms send success: {}", phones);
        } catch (Exception e) {
            LOG.error("Alert event sms send error: " + phones, e);
        }
    }

}

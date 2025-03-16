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

import java.util.Map;

/**
 * Lark alert sender
 *
 * @author Ponfee
 */
public class LarkAlertSender extends AlertSender {

    public static final String CHANNEL = "lark";

    public LarkAlertSender(LarkAlertSenderProperties config, LarkUserRecipientMapper mapper) {
        super(CHANNEL, "飞书", mapper);

        // TODO: init Mail client by config
    }

    @Override
    protected void doSend(AlertEvent alertEvent, Map<String, String> alertRecipients, String webhook) {

    }

}

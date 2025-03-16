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

package cn.ponfee.disjob.alert;

import cn.ponfee.disjob.alert.enums.AlertType;
import cn.ponfee.disjob.alert.event.AlertEvent;
import cn.ponfee.disjob.alert.sender.AlertSender;
import cn.ponfee.disjob.common.concurrent.Threads;
import cn.ponfee.disjob.core.base.GroupInfoService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Alerter
 *
 * @author Ponfee
 */
public class Alerter {

    private static final Logger LOG = LoggerFactory.getLogger(Alerter.class);

    private final Map<AlertType, String[]> typeChannelMap;
    private final GroupInfoService groupInfoService;
    private final Executor executor;

    public Alerter(AlerterProperties config,
                   GroupInfoService groupInfoService,
                   Executor executor) {
        Assert.notNull(config, "Alerter config cannot be empty.");
        this.typeChannelMap = ObjectUtils.defaultIfNull(config.getTypeChannelMap(), Collections.emptyMap());
        this.groupInfoService = groupInfoService;
        this.executor = Objects.requireNonNull(executor);
    }

    public void alert(AlertEvent event) {
        String[] channels = typeChannelMap.get(event.getAlertType());
        if (channels == null || channels.length == 0) {
            return;
        }

        Set<String> alertUsers = groupInfoService.getAlertUsers(event.getGroup());
        String webhook = groupInfoService.getWebhook(event.getGroup());
        if (CollectionUtils.isEmpty(alertUsers) && StringUtils.isBlank(webhook)) {
            return;
        }

        // TODO 限流：滑动窗口、漏斗算法、分布式集群限流

        executor.execute(() -> {
            try {
                for (String channel : channels) {
                    AlertSender alertSender = AlertSender.getAlertSender(channel);
                    alertSender.send(event, alertUsers, webhook);
                }
            } catch (Throwable t) {
                Threads.interruptIfNecessary(t);
                LOG.error("Send alert event message occur error: " + event, t);
            }
        });
    }

}

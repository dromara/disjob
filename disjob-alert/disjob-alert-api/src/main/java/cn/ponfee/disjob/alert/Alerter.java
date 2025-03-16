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

import cn.ponfee.disjob.alert.configuration.AlerterProperties;
import cn.ponfee.disjob.alert.configuration.AlerterProperties.SendRateLimit;
import cn.ponfee.disjob.alert.enums.AlertType;
import cn.ponfee.disjob.alert.event.AlertEvent;
import cn.ponfee.disjob.alert.sender.AlertSender;
import cn.ponfee.disjob.common.collect.SlidingWindow;
import cn.ponfee.disjob.common.concurrent.Threads;
import cn.ponfee.disjob.core.base.GroupInfoService;
import cn.ponfee.disjob.core.base.JobConstants;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Alerter
 *
 * @author Ponfee
 */
public class Alerter {

    /**
     * Alert key prefix
     */
    public static final String KEY_PREFIX = JobConstants.DISJOB_KEY_PREFIX + ".alert";

    /**
     * Alert sender config key prefix
     */
    public static final String SENDER_CONFIG_KEY_PREFIX = KEY_PREFIX + ".sender";

    /**
     * Alert UserRecipientMapper spring bean name prefix
     */
    public static final String USER_RECIPIENT_MAPPER_BEAN_NAME_PREFIX = KEY_PREFIX + ".user_recipient_mapper";

    private static final Logger LOG = LoggerFactory.getLogger(Alerter.class);

    /**
     * 配置告警类型使用的渠道列表
     */
    private final Map<AlertType, String[]> typeChannelsMap;

    /**
     * 通过group获取告警接收人信息（alertUsers、webhook等）
     */
    private final GroupInfoService groupInfoService;

    /**
     * 告警发送执行器（异步执行线程池）
     */
    private final Executor executor;

    /**
     * Send rate limit config
     */
    private final SendRateLimit limit;

    /**
     * 告警限流
     */
    private final ConcurrentHashMap<String, SlidingWindow> rateLimiterMap = new ConcurrentHashMap<>();

    public Alerter(AlerterProperties config, GroupInfoService groupInfoService, Executor executor) {
        Assert.notNull(config, "Alerter config cannot be empty.");
        this.typeChannelsMap = config.getTypeChannelsMap();
        this.groupInfoService = groupInfoService;
        this.executor = Objects.requireNonNull(executor);
        this.limit = config.getSendRateLimit();
    }

    public void alert(AlertEvent event) {
        String[] channels = typeChannelsMap.get(event.getAlertType());
        if (channels == null || channels.length == 0) {
            return;
        }

        Set<String> alertUsers = groupInfoService.getAlertUsers(event.getGroup());
        String webhook = groupInfoService.getWebhook(event.getGroup());
        if (CollectionUtils.isEmpty(alertUsers) && StringUtils.isBlank(webhook)) {
            return;
        }

        SlidingWindow slidingWindow = rateLimiterMap.computeIfAbsent(
            event.buildRateLimitKey(),
            key -> new SlidingWindow(limit.getMaxRequests(), limit.getWindowSizeInMillis())
        );
        if (!slidingWindow.tryAcquire()) {
            LOG.warn("Alert event rate limited: {}", event);
            return;
        }

        try {
            doAlert(channels, event, alertUsers, webhook);
        } catch (Throwable t) {
            // if RejectedExecutionException or other exception
            LOG.warn("Alert event execute error: {}, {}", event, t.getMessage());
            Threads.interruptIfNecessary(t);
        }
    }

    private void doAlert(String[] channels, AlertEvent event, Set<String> alertUsers, String webhook) {
        executor.execute(() -> {
            try {
                for (String channel : channels) {
                    AlertSender sender = AlertSender.get(channel);
                    if (sender != null) {
                        sender.send(event, alertUsers, webhook);
                    }
                }
            } catch (Throwable t) {
                LOG.error("Alert event send error: " + event, t);
                Threads.interruptIfNecessary(t);
            }
        });
    }

}

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final ConcurrentHashMap<AlertType, SlidingWindow> rateLimiter = new ConcurrentHashMap<>();
    private final Integer MaxRequests = 5; // 每分钟最多执行次数


    public Alerter(AlerterProperties config,
                   GroupInfoService groupInfoService,
                   Executor executor) {
        Assert.notNull(config, "Alerter config cannot be empty.");
        this.typeChannelMap = ObjectUtils.defaultIfNull(config.getTypeChannelMap(), Collections.emptyMap());
        this.groupInfoService = groupInfoService;
        this.executor = Objects.requireNonNull(executor);
        for (AlertType alertType : typeChannelMap.keySet()) {
            rateLimiter.put(alertType, new SlidingWindow(MaxRequests, 60000)); // 每分钟最多允许 5 次告警
        }
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
        // 滑动窗口
        // TODO 待讨论：限流维度为group或job级别
        SlidingWindow slidingWindow = rateLimiter.get(event.getAlertType());
        if (slidingWindow != null && !slidingWindow.tryAcquire()) {
            LOG.warn("Alert rate limited for event: {}", event);
            return; // 如果限流器判定超出限制，则直接返回
        }

        executor.execute(() -> {
            try {
                for (String channel : channels) {
                    AlertSender sender = AlertSender.getAlertSender(channel);
                    if (sender != null) {
                        sender.send(event, alertUsers, webhook);
                    }
                }
            } catch (Throwable t) {
                Threads.interruptIfNecessary(t);
                LOG.error("Send alert event message occur error: " + event, t);
            }
        });
    }

    /**
     * Sliding window implementation class
     */
    private static class SlidingWindow {
        private final int maxRequests; // maximum number of requests
        private final long windowSizeInMillis; // Window size (milliseconds)
        private final AtomicInteger requestCount; // Current request count
        private volatile long windowStart; // window start time

        public SlidingWindow(int maxRequests, long windowSizeInMillis) {
            this.maxRequests = maxRequests;
            this.windowSizeInMillis = windowSizeInMillis;
            this.requestCount = new AtomicInteger(0);
            this.windowStart = System.currentTimeMillis();
        }

        /**
         * Try to obtain a request quota.
         */
        public synchronized boolean tryAcquire() {
            long now = System.currentTimeMillis();
            if (now - windowStart > windowSizeInMillis) {
                windowStart = now;
                requestCount.set(0);
            }
            if (requestCount.get() < maxRequests) {
                requestCount.incrementAndGet();
                return true;
            }
            return false;
        }
    }
}

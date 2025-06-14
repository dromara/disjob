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

import cn.ponfee.disjob.alert.base.AlertEvent;
import cn.ponfee.disjob.alert.base.AlerterProperties;
import cn.ponfee.disjob.alert.base.AlerterProperties.SendRateLimit;
import cn.ponfee.disjob.alert.sender.AlertSender;
import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.collect.SlidingWindow;
import cn.ponfee.disjob.common.concurrent.NamedThreadFactory;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.core.base.GroupInfoService;
import cn.ponfee.disjob.core.base.JobConstants;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.event.EventListener;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Alerter
 *
 * @author Ponfee
 */
public class Alerter extends SingletonClassConstraint implements DisposableBean {

    /**
     * Alert key prefix
     */
    public static final String KEY_PREFIX = JobConstants.DISJOB_KEY_PREFIX + ".alert";

    /**
     * Alert enabled key expression
     */
    public static final String ENABLED_KEY_EXPRESSION = "${" + KEY_PREFIX + ".enabled:true}";

    /**
     * Alert sender config key prefix
     */
    public static final String SENDER_CONFIG_KEY_PREFIX = KEY_PREFIX + ".sender";

    /**
     * AlertRecipientMapper spring bean name prefix
     */
    public static final String RECIPIENT_MAPPER_BEAN_NAME_PREFIX = KEY_PREFIX + ".recipient_mapper";

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(Alerter.class);

    /**
     * The alerter config
     */
    private final AlerterProperties config;

    /**
     * 通过group获取告警接收人信息（alertRecipients、alertWebhook等）
     */
    private final GroupInfoService groupInfoService;

    /**
     * 异步发送线程池：警报消息
     */
    private final ThreadPoolExecutor alarmAsyncExecutor;

    /**
     * 异步发送线程池：通知消息
     */
    private final ThreadPoolExecutor noticeAsyncExecutor;

    /**
     * 告警限流器
     */
    private final ConcurrentHashMap<String, SlidingWindow> rateLimiterMap = new ConcurrentHashMap<>();

    public Alerter(AlerterProperties config, GroupInfoService groupInfoService) {
        this.config = Objects.requireNonNull(config, "Alerter config cannot be null.");
        this.groupInfoService = Objects.requireNonNull(groupInfoService, "Group info service cannot be null.");
        this.alarmAsyncExecutor = createThreadPoolExecutor(config);
        this.noticeAsyncExecutor = createThreadPoolExecutor(config);
    }

    @EventListener
    public void onAlertEvent(AlertEvent event) {
        try {
            alert(event);
        } catch (Throwable t) {
            LOG.warn("Alert event occur error: " + event, t);
        }
    }

    @Override
    public void destroy() throws Exception {
        ThreadPoolExecutors.shutdown(noticeAsyncExecutor, config.getSendThreadPool().getAwaitTerminationSeconds());
        ThreadPoolExecutors.shutdown(alarmAsyncExecutor, config.getSendThreadPool().getAwaitTerminationSeconds());
    }

    // ------------------------------------------------------------------private methods

    private static ThreadPoolExecutor createThreadPoolExecutor(AlerterProperties config) {
        AlerterProperties.SendThreadPool pool = config.getSendThreadPool();
        return ThreadPoolExecutors.builder()
            .corePoolSize(pool.getCorePoolSize())
            .maximumPoolSize(pool.getMaximumPoolSize())
            .workQueue(new LinkedBlockingQueue<>(pool.getQueueCapacity()))
            .keepAliveTimeSeconds(pool.getKeepAliveTimeSeconds())
            .allowCoreThreadTimeOut(pool.isAllowCoreThreadTimeOut())
            .threadFactory(NamedThreadFactory.builder().prefix("alert_async_send_thread").build())
            .rejectedHandler((task, executor) -> LOG.warn("Alert event be discard: {}", ((AlertTask) task).event))
            .build();
    }

    private void alert(AlertEvent event) {
        String[] channels = config.getTypeChannelsMap().get(event.getAlertType());
        if (ArrayUtils.isEmpty(channels)) {
            return;
        }
        Set<String> alertRecipients = groupInfoService.getAlertRecipients(event.getGroup());
        String alertWebhook = groupInfoService.getAlertWebhook(event.getGroup());
        if (CollectionUtils.isEmpty(alertRecipients) && StringUtils.isBlank(alertWebhook)) {
            return;
        }
        ThreadPoolExecutor executor = event.getAlertType().isAlarm() ? alarmAsyncExecutor : noticeAsyncExecutor;
        executor.execute(new AlertTask(event, channels, alertRecipients, alertWebhook));
    }

    private class AlertTask implements Runnable {
        private final AlertEvent event;
        private final String[] channels;
        private final Set<String> alertRecipients;
        private final String alertWebhook;

        AlertTask(AlertEvent event, String[] channels, Set<String> alertRecipients, String alertWebhook) {
            this.event = event;
            this.channels = channels;
            this.alertRecipients = alertRecipients;
            this.alertWebhook = alertWebhook;
        }

        @Override
        public void run() {
            SendRateLimit sendRateLimit = config.getSendRateLimit();
            SlidingWindow slidingWindow = rateLimiterMap.computeIfAbsent(
                event.buildRateLimitKey(),
                key -> new SlidingWindow(sendRateLimit.getMaxRequests(), sendRateLimit.getWindowSizeInMillis())
            );
            if (!slidingWindow.tryAcquire()) {
                LOG.warn("Alert event rate limited: {}", event);
                return;
            }

            Arrays.stream(channels).map(AlertSender::get).filter(Objects::nonNull).forEach(sender -> {
                try {
                    sender.send(event, alertRecipients, alertWebhook);
                } catch (Throwable t) {
                    LOG.error("Alert event send error: " + event, t);
                }
            });
        }
    }

}

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

package cn.ponfee.disjob.alert.configuration;

import cn.ponfee.disjob.alert.Alerter;
import cn.ponfee.disjob.alert.AlerterProperties;
import cn.ponfee.disjob.alert.configuration.EnableAlerter.EnableAlerterConfiguration;
import cn.ponfee.disjob.common.concurrent.NamedThreadFactory;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.core.base.GroupInfoService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Enable alerter
 *
 * @author Ponfee
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(EnableAlerterConfiguration.class)
public @interface EnableAlerter {

    @EnableConfigurationProperties(AlerterProperties.class)
    class EnableAlerterConfiguration implements DisposableBean {

        private final AlerterProperties config;
        private final ThreadPoolExecutor alertSendExecutor;

        EnableAlerterConfiguration(AlerterProperties config) {
            this.config = config;

            AlerterProperties.TaskExecutionPool pool = config.getTaskExecutionPool();
            this.alertSendExecutor = ThreadPoolExecutors.builder()
                .corePoolSize(pool.getCorePoolSize())
                .maximumPoolSize(pool.getMaximumPoolSize())
                .keepAliveTimeSeconds(pool.getKeepAliveTimeSeconds())
                .workQueue(new LinkedBlockingQueue<>(pool.getQueueCapacity()))
                .allowCoreThreadTimeOut(pool.isAllowCoreThreadTimeOut())
                .threadFactory(NamedThreadFactory.builder().prefix("alert_send_thread").build())
                .rejectedHandler(ThreadPoolExecutors.ABORT)
                .build();
        }

        @Bean
        public Alerter alerter(GroupInfoService groupInfoService) {
            return new Alerter(config, groupInfoService, alertSendExecutor);
        }

        @Override
        public void destroy() throws Exception {
            int awaitTerminationSeconds = config.getTaskExecutionPool().getAwaitTerminationSeconds();
            ThreadPoolExecutors.shutdown(alertSendExecutor, awaitTerminationSeconds);
        }
    }

}

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

package cn.ponfee.disjob.supervisor.configuration;

import cn.ponfee.disjob.alert.Alerter;
import cn.ponfee.disjob.alert.base.AlerterProperties;
import cn.ponfee.disjob.alert.sender.AlertSender;
import cn.ponfee.disjob.common.lock.DatabaseLockTemplate;
import cn.ponfee.disjob.common.lock.LockTemplate;
import cn.ponfee.disjob.core.base.GroupInfoService;
import cn.ponfee.disjob.supervisor.base.DefaultGroupInfoService;
import cn.ponfee.disjob.supervisor.base.SupervisorConstants;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.jdbc.core.JdbcTemplate;

import static cn.ponfee.disjob.supervisor.dao.SupervisorDataSourceConfig.SPRING_BEAN_NAME_JDBC_TEMPLATE;

/**
 * Supervisor DeferredImportSelector
 *
 * @author Ponfee
 */
class SupervisorDeferredImportSelector implements DeferredImportSelector {

    @SuppressWarnings("NullableProblems")
    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[]{SupervisorDeferredConfiguration.class.getName()};
    }

    private static class SupervisorDeferredConfiguration {

        @ConditionalOnMissingBean
        @Bean
        public GroupInfoService groupInfoService() {
            return DefaultGroupInfoService.INSTANCE;
        }

        @ConditionalOnMissingBean(name = SupervisorConstants.SPRING_BEAN_NAME_SCAN_WAITING_INSTANCE_LOCKER)
        @Bean(SupervisorConstants.SPRING_BEAN_NAME_SCAN_WAITING_INSTANCE_LOCKER)
        public LockTemplate scanWaitingInstanceLocker(@Qualifier(SPRING_BEAN_NAME_JDBC_TEMPLATE) JdbcTemplate jdbcTemplate) {
            return new DatabaseLockTemplate(jdbcTemplate, SupervisorConstants.LOCK_SCAN_WAITING_INSTANCE);
        }

        @ConditionalOnMissingBean(name = SupervisorConstants.SPRING_BEAN_NAME_SCAN_RUNNING_INSTANCE_LOCKER)
        @Bean(SupervisorConstants.SPRING_BEAN_NAME_SCAN_RUNNING_INSTANCE_LOCKER)
        public LockTemplate scanRunningInstanceLocker(@Qualifier(SPRING_BEAN_NAME_JDBC_TEMPLATE) JdbcTemplate jdbcTemplate) {
            return new DatabaseLockTemplate(jdbcTemplate, SupervisorConstants.LOCK_SCAN_RUNNING_INSTANCE);
        }

        @ConditionalOnMissingBean(name = SupervisorConstants.SPRING_BEAN_NAME_SCAN_TRIGGERING_JOB_LOCKER)
        @Bean(SupervisorConstants.SPRING_BEAN_NAME_SCAN_TRIGGERING_JOB_LOCKER)
        public LockTemplate scanTriggeringJobLocker(@Qualifier(SPRING_BEAN_NAME_JDBC_TEMPLATE) JdbcTemplate jdbcTemplate) {
            return new DatabaseLockTemplate(jdbcTemplate, SupervisorConstants.LOCK_SCAN_TRIGGERING_JOB);
        }

        @ConditionalOnExpression(Alerter.ENABLED_KEY_EXPRESSION)
        @ConditionalOnBean(AlertSender.class)
        @Bean
        public AlerterProperties alerterProperties() {
            return new AlerterProperties();
        }

        @ConditionalOnExpression(Alerter.ENABLED_KEY_EXPRESSION)
        @ConditionalOnBean(AlertSender.class)
        @Bean
        public Alerter alerter(AlerterProperties alerterConfig, GroupInfoService groupInfoService) {
            return new Alerter(alerterConfig, groupInfoService);
        }
    }

}

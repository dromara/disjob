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

package cn.ponfee.disjob.registry.database.configuration;

import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.spring.JdbcTemplateWrapper;
import cn.ponfee.disjob.common.util.ObjectUtils;
import cn.ponfee.disjob.common.util.Strings;
import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.registry.configuration.BaseServerRegistryAutoConfiguration;
import cn.ponfee.disjob.registry.database.DatabaseSupervisorRegistry;
import cn.ponfee.disjob.registry.database.DatabaseWorkerRegistry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

/**
 * Spring autoconfiguration for database server registry
 *
 * @author Ponfee
 */
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@EnableConfigurationProperties(DatabaseRegistryProperties.class)
public class DatabaseServerRegistryAutoConfiguration extends BaseServerRegistryAutoConfiguration implements DisposableBean {

    /**
     * Database registry JdbcTemplateWrapper spring bean name
     */
    public static final String SPRING_BEAN_NAME_JDBC_TEMPLATE_WRAPPER = JobConstants.SPRING_BEAN_NAME_PREFIX + ".registry.database.jdbc-template-wrapper";

    /**
     * Data source holder
     */
    private final Mutable<HikariDataSource> dataSourceHolder = new MutableObject<>();

    /**
     * Configuration database registry datasource.
     *
     * 当`havingValue=""`时，只要`属性有值&&不为false`则生效。
     */
    @ConditionalOnProperty(name = DatabaseRegistryProperties.KEY_PREFIX + ".datasource.url")
    @ConditionalOnMissingBean(name = SPRING_BEAN_NAME_JDBC_TEMPLATE_WRAPPER)
    @Bean(SPRING_BEAN_NAME_JDBC_TEMPLATE_WRAPPER)
    public JdbcTemplateWrapper databaseRegistryJdbcTemplateWrapper(DatabaseRegistryProperties props) {
        DatabaseRegistryProperties.DataSourceConfig p = props.getDatasource();
        HikariConfig cfg = new HikariConfig();
        cfg.setDriverClassName(StringUtils.getIfBlank(p.getDriverClassName(), () -> DatabaseDriver.fromJdbcUrl(p.getUrl()).getDriverClassName()));
        cfg.setJdbcUrl(p.getUrl());
        cfg.setUsername(p.getUsername());
        cfg.setPassword(p.getPassword());

        // apply hikari config
        DatabaseRegistryProperties.Hikari hikari = p.getHikari();
        cfg.setMinimumIdle(hikari.getMinimumIdle());
        cfg.setMaximumPoolSize(hikari.getMaximumPoolSize());
        cfg.setConnectionTimeout(hikari.getConnectionTimeout());
        cfg.setIdleTimeout(hikari.getIdleTimeout());
        cfg.setMaxLifetime(hikari.getMaxLifetime());
        Strings.applyIfNotBlank(hikari.getPoolName(), cfg::setPoolName);
        Strings.applyIfNotBlank(hikari.getCatalog(), cfg::setCatalog);
        Strings.applyIfNotBlank(hikari.getSchema(), cfg::setSchema);
        Strings.applyIfNotBlank(hikari.getConnectionTestQuery(), cfg::setConnectionTestQuery);
        Strings.applyIfNotBlank(hikari.getConnectionInitSql(), cfg::setConnectionInitSql);
        Strings.applyIfNotBlank(hikari.getTransactionIsolation(), cfg::setTransactionIsolation);
        Strings.applyIfNotBlank(hikari.getExceptionOverrideClassName(), cfg::setExceptionOverrideClassName);
        ObjectUtils.applyIfNotNull(hikari.getValidationTimeout(), cfg::setValidationTimeout);
        ObjectUtils.applyIfNotNull(hikari.getLeakDetectionThreshold(), cfg::setLeakDetectionThreshold);
        ObjectUtils.applyIfNotNull(hikari.getInitializationFailTimeout(), cfg::setInitializationFailTimeout);
        ObjectUtils.applyIfNotNull(hikari.getKeepaliveTime(), cfg::setKeepaliveTime);
        ObjectUtils.applyIfNotNull(hikari.getAutoCommit(), cfg::setAutoCommit);
        ObjectUtils.applyIfNotNull(hikari.getReadOnly(), cfg::setReadOnly);
        ObjectUtils.applyIfNotNull(hikari.getIsolateInternalQueries(), cfg::setIsolateInternalQueries);
        ObjectUtils.applyIfNotNull(hikari.getRegisterMbeans(), cfg::setRegisterMbeans);
        ObjectUtils.applyIfNotNull(hikari.getAllowPoolSuspension(), cfg::setAllowPoolSuspension);

        // create hikari DataSource
        HikariDataSource dataSource = new HikariDataSource(cfg);
        dataSourceHolder.setValue(dataSource);
        return JdbcTemplateWrapper.of(new JdbcTemplate(dataSource));
    }

    /**
     * Configuration database supervisor registry.
     */
    @ConditionalOnBean(Supervisor.Local.class)
    @Bean
    public SupervisorRegistry supervisorRegistry(DatabaseRegistryProperties config,
                                                 @Qualifier(JobConstants.SPRING_BEAN_NAME_REST_TEMPLATE) RestTemplate restTemplate,
                                                 @Qualifier(SPRING_BEAN_NAME_JDBC_TEMPLATE_WRAPPER) JdbcTemplateWrapper wrapper) {
        return new DatabaseSupervisorRegistry(config, restTemplate, wrapper);
    }

    /**
     * Configuration database worker registry.
     */
    @ConditionalOnBean(Worker.Local.class)
    @Bean
    public WorkerRegistry workerRegistry(DatabaseRegistryProperties config,
                                         @Qualifier(JobConstants.SPRING_BEAN_NAME_REST_TEMPLATE) RestTemplate restTemplate,
                                         @Qualifier(SPRING_BEAN_NAME_JDBC_TEMPLATE_WRAPPER) JdbcTemplateWrapper wrapper) {
        return new DatabaseWorkerRegistry(config, restTemplate, wrapper);
    }

    @Override
    public void destroy() {
        HikariDataSource dataSource = dataSourceHolder.getValue();
        if (dataSource != null) {
            log.info("Database registry datasource destroy begin.");
            ThrowingRunnable.doCaught(dataSource::close, "Database registry datasource destroy error: {}");
            log.info("Database registry datasource destroy end.");
        }
    }

}

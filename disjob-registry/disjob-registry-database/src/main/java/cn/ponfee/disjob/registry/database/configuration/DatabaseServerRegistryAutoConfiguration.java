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

import cn.ponfee.disjob.common.base.Destroyable;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.spring.JdbcTemplateWrapper;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.registry.configuration.BaseServerRegistryAutoConfiguration;
import cn.ponfee.disjob.registry.database.DatabaseSupervisorRegistry;
import cn.ponfee.disjob.registry.database.DatabaseWorkerRegistry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static cn.ponfee.disjob.core.base.JobConstants.SPRING_BEAN_NAME_PREFIX;

/**
 * Spring autoconfiguration for database server registry
 *
 * @author Ponfee
 */
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@EnableConfigurationProperties(DatabaseRegistryProperties.class)
public class DatabaseServerRegistryAutoConfiguration extends BaseServerRegistryAutoConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseServerRegistryAutoConfiguration.class);

    /**
     * Database registry JdbcTemplateWrapper spring bean name
     */
    public static final String REGISTRY_DATABASE_JDBC_TEMPLATE_WRAPPER = SPRING_BEAN_NAME_PREFIX + ".registry.database.jdbc-template-wrapper";

    /**
     * Configuration database registry datasource.
     */
    @ConditionalOnMissingBean(name = REGISTRY_DATABASE_JDBC_TEMPLATE_WRAPPER)
    @Bean(REGISTRY_DATABASE_JDBC_TEMPLATE_WRAPPER)
    public JdbcTemplateWrapper databaseRegistryJdbcTemplateWrapper(DatabaseRegistryProperties config) {
        DatabaseRegistryProperties.DataSourceProperties props = config.getDatasource();
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(props.getDriverClassName());
        hikariConfig.setJdbcUrl(props.getJdbcUrl());
        hikariConfig.setUsername(props.getUsername());
        hikariConfig.setPassword(props.getPassword());
        hikariConfig.setAutoCommit(props.isAutoCommit());
        hikariConfig.setMinimumIdle(props.getMinimumIdle());
        hikariConfig.setIdleTimeout(props.getIdleTimeout());
        hikariConfig.setMaximumPoolSize(props.getMaximumPoolSize());
        hikariConfig.setMaxLifetime(props.getMaxLifetime());
        hikariConfig.setConnectionTimeout(props.getConnectionTimeout());
        hikariConfig.setConnectionTestQuery(props.getConnectionTestQuery());
        hikariConfig.setPoolName(props.getPoolName());
        DataSource dataSource = new HikariDataSource(hikariConfig);
        return JdbcTemplateWrapper.of(new JdbcTemplate(dataSource));
    }

    /**
     * Configuration database supervisor registry.
     */
    @ConditionalOnBean(Supervisor.Current.class)
    @Bean
    public SupervisorRegistry supervisorRegistry(DatabaseRegistryProperties config,
                                                 @Qualifier(REGISTRY_DATABASE_JDBC_TEMPLATE_WRAPPER) JdbcTemplateWrapper wrapper) {
        return new DatabaseSupervisorRegistry(config, wrapper);
    }

    /**
     * Configuration database worker registry.
     */
    @ConditionalOnBean(Worker.Current.class)
    @Bean
    public WorkerRegistry workerRegistry(DatabaseRegistryProperties config,
                                         @Qualifier(REGISTRY_DATABASE_JDBC_TEMPLATE_WRAPPER) JdbcTemplateWrapper wrapper) {
        return new DatabaseWorkerRegistry(config, wrapper);
    }

    // -------------------------------------------------------------------------destroy datasource

    @ConditionalOnBean(name = REGISTRY_DATABASE_JDBC_TEMPLATE_WRAPPER)
    @Bean
    private DataSourceDestroy dataSourceDestroy(@Qualifier(REGISTRY_DATABASE_JDBC_TEMPLATE_WRAPPER) JdbcTemplateWrapper wrapper) {
        return new DataSourceDestroy(wrapper.jdbcTemplate());
    }

    private static class DataSourceDestroy implements DisposableBean {
        final JdbcTemplate jdbcTemplate;

        DataSourceDestroy(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @Override
        public void destroy() {
            DataSource dataSource = jdbcTemplate.getDataSource();
            if (dataSource != null) {
                LOG.info("Database registry datasource destroy begin.");
                ThrowingRunnable.doCaught(() -> Destroyable.destroy(dataSource), () -> "Database registry datasource destroy error.");
                LOG.info("Database registry datasource destroy end.");
            }
        }
    }

}

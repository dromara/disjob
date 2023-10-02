/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.registry.database.configuration;

import cn.ponfee.disjob.common.base.Releasable;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.spring.JdbcTemplateWrapper;
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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Spring autoconfiguration for database server registry
 *
 * @author Ponfee
 */
@EnableConfigurationProperties(DatabaseRegistryProperties.class)
public class DatabaseServerRegistryAutoConfiguration extends BaseServerRegistryAutoConfiguration {

    private static final String SPRING_BEAN_NAME_JTW = JobConstants.SPRING_BEAN_NAME_PREFIX + ".database-registry-jtw";

    /**
     * Configuration database registry datasource.
     */
    @ConditionalOnMissingBean
    @Bean(SPRING_BEAN_NAME_JTW)
    public JdbcTemplateWrapper databaseRegistryJdbcTemplateWrapper(DatabaseRegistryProperties config) {
        DatabaseRegistryProperties.DataSourceProperties props = config.getDatasource();
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(props.getDriverClassName());
        hikariConfig.setJdbcUrl(props.getJdbcUrl());
        hikariConfig.setUsername(props.getUsername());
        hikariConfig.setPassword(props.getPassword());
        hikariConfig.setMinimumIdle(props.getMinimumIdle());
        hikariConfig.setMaximumPoolSize(props.getMaximumPoolSize());
        hikariConfig.setConnectionTimeout(props.getConnectionTimeout());
        hikariConfig.setPoolName(props.getPoolName());
        DataSource dataSource = new HikariDataSource(hikariConfig);
        return JdbcTemplateWrapper.of(new JdbcTemplate(dataSource));
    }

    /**
     * Configuration database supervisor registry.
     */
    @ConditionalOnBean(Supervisor.class)
    @ConditionalOnMissingBean
    @Bean
    public SupervisorRegistry supervisorRegistry(DatabaseRegistryProperties config,
                                                 @Qualifier(SPRING_BEAN_NAME_JTW) JdbcTemplateWrapper wrapper) {
        return new DatabaseSupervisorRegistry(config, wrapper);
    }

    /**
     * Configuration database worker registry.
     */
    @ConditionalOnBean(Worker.class)
    @ConditionalOnMissingBean
    @Bean
    public WorkerRegistry workerRegistry(DatabaseRegistryProperties config,
                                         @Qualifier(SPRING_BEAN_NAME_JTW) JdbcTemplateWrapper wrapper) {
        return new DatabaseWorkerRegistry(config, wrapper);
    }

    // -------------------------------------------------------------------------destroy datasource

    @ConditionalOnMissingBean
    @Bean
    private DatabaseRegistryDataSourceDestroy databaseRegistryDataSourceDestroy(@Qualifier(SPRING_BEAN_NAME_JTW) JdbcTemplateWrapper wrapper) {
        return new DatabaseRegistryDataSourceDestroy(wrapper);
    }

    private class DatabaseRegistryDataSourceDestroy implements DisposableBean {
        final JdbcTemplateWrapper wrapper;

        DatabaseRegistryDataSourceDestroy(JdbcTemplateWrapper wrapper) {
            this.wrapper = wrapper;
        }

        @Override
        public void destroy() throws Exception {
            DataSource dataSource = wrapper.jdbcTemplate().getDataSource();
            if (dataSource != null) {
                ThrowingRunnable.execute(() -> Releasable.release(dataSource), () -> "Database registry datasource destroy error.");
                log.info("Database registry datasource destroy finished.");
            }
        }
    }

}

/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.dao;

import cn.ponfee.scheduler.common.util.ClassUtils;
import cn.ponfee.scheduler.supervisor.base.AbstractDataSourceConfig;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/**
 * Supervisor datasource configuration
 *
 * <pre>
 *  spring.datasource.distributed-scheduler:
 *    driver-class-name: com.mysql.cj.jdbc.Driver
 *    jdbc-url: jdbc:mysql://112.74.170.75:3306/distributed_scheduler?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&&autoReconnect=true&failOverReadOnly=false&connectTimeout=2000&socketTimeout=5000
 *    username: root
 *    password:
 *    minimum-idle: 10
 *    maximum-pool-size: 100
 *    connection-timeout: 2000
 *    pool-name: distributed_scheduler
 * </pre>
 *
 * @author Ponfee
 */
@Configuration
@MapperScan(
    basePackages = SupervisorDataSourceConfig.BASE_PACKAGE + ".mapper",
    sqlSessionTemplateRef = SupervisorDataSourceConfig.DB_NAME + AbstractDataSourceConfig.SQL_SESSION_TEMPLATE_SUFFIX
)
public class SupervisorDataSourceConfig extends AbstractDataSourceConfig {

    /**
     * Package path
     *
     * @see ClassUtils#getPackagePath(Class)
     */
    public static final String BASE_PACKAGE = "cn.ponfee.scheduler.supervisor.dao";

    /**
     * database name
     */
    public static final String DB_NAME = "scheduler";

    @Bean(name = DB_NAME + DATA_SOURCE_SUFFIX)
    @ConfigurationProperties(prefix = "spring.datasource.distributed-scheduler")
    @Override
    public DataSource dataSource() {
        // return new com.zaxxer.hikari.HikariDataSource();
        return DataSourceBuilder.create().build();
    }

    @Bean(name = DB_NAME + SQL_SESSION_FACTORY_SUFFIX)
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        return super.createSqlSessionFactory();
    }

    @Bean(name = DB_NAME + SQL_SESSION_TEMPLATE_SUFFIX)
    public SqlSessionTemplate sqlSessionTemplate() throws Exception {
        return new SqlSessionTemplate(sqlSessionFactory());
    }

    @Bean(name = DB_NAME + TX_MANAGER_SUFFIX)
    public DataSourceTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource());
    }

    @Bean(name = DB_NAME + TX_TEMPLATE_SUFFIX)
    public TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(transactionManager());
    }

    @Bean(name = DB_NAME + JDBC_TEMPLATE_SUFFIX)
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource());
    }
}

/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.dao;

import cn.ponfee.disjob.supervisor.base.AbstractDataSourceConfig;
import org.apache.commons.lang3.ClassUtils;
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
 *  disjob.datasource:
 *    driver-class-name: com.mysql.cj.jdbc.Driver
 *    jdbc-url: jdbc:mysql://112.74.170.75:3306/disjob?allowMultiQueries=true&useUnicode=true&characterEncoding=UTF-8&useSSL=false&autoReconnect=true&connectTimeout=2000&socketTimeout=5000&serverTimezone=Asia/Shanghai&failOverReadOnly=false
 *    username: disjob
 *    password:
 *    minimum-idle: 10
 *    maximum-pool-size: 100
 *    connection-timeout: 2000
 *    pool-name: disjob
 * </pre>
 *
 * @author Ponfee
 */
@Configuration
@MapperScan(
    basePackages = SupervisorDataSourceConfig.BASE_PACKAGE + ".mapper",
    sqlSessionTemplateRef = SupervisorDataSourceConfig.DB_NAME + AbstractDataSourceConfig.SQL_SESSION_TEMPLATE_NAME_SUFFIX
)
public class SupervisorDataSourceConfig extends AbstractDataSourceConfig {

    /**
     * Package path
     *
     * @see ClassUtils#getPackageName(Class)
     */
    static final String BASE_PACKAGE = "cn.ponfee.disjob.supervisor.dao";
    static {
        // Check package name
        if (!BASE_PACKAGE.equals(ClassUtils.getPackageName(SupervisorDataSourceConfig.class))) {
            throw new Error("Invalid package path of " + SupervisorDataSourceConfig.class);
        }
    }

    /**
     * database name
     */
    public static final String DB_NAME = "disjob";

    @Bean(name = DB_NAME + DATA_SOURCE_NAME_SUFFIX)
    @ConfigurationProperties(prefix = DB_NAME + ".datasource")
    @Override
    public DataSource dataSource() {
        return DataSourceBuilder.create()
            //.type(com.zaxxer.hikari.HikariDataSource.class)
            .build();
    }

    @Bean(name = DB_NAME + SQL_SESSION_FACTORY_NAME_SUFFIX)
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        return super.createSqlSessionFactory();
    }

    @Bean(name = DB_NAME + SQL_SESSION_TEMPLATE_NAME_SUFFIX)
    public SqlSessionTemplate sqlSessionTemplate() throws Exception {
        return new SqlSessionTemplate(sqlSessionFactory());
    }

    @Bean(name = DB_NAME + TX_MANAGER_NAME_SUFFIX)
    public DataSourceTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource());
    }

    @Bean(name = DB_NAME + TX_TEMPLATE_NAME_SUFFIX)
    public TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(transactionManager());
    }

    @Bean(name = DB_NAME + JDBC_TEMPLATE_NAME_SUFFIX)
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource());
    }

}

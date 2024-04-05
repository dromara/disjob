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

package cn.ponfee.disjob.supervisor.dao;

import cn.ponfee.disjob.supervisor.base.AbstractDataSourceConfig;
import cn.ponfee.disjob.supervisor.configuration.SupervisorProperties;
import org.apache.commons.lang3.ClassUtils;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 *    jdbc-url: jdbc:mysql://localhost:3306/disjob?useUnicode=true&characterEncoding=UTF-8&useSSL=false&autoReconnect=true&connectTimeout=2000&socketTimeout=5000&serverTimezone=Asia/Shanghai&failOverReadOnly=false
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
@ConditionalOnProperty(prefix = SupervisorProperties.KEY_PREFIX + ".datasource", name = "custom", havingValue = "false", matchIfMissing = true)
@Configuration
@MapperScan(
    basePackages = SupervisorDataSourceConfig.BASE_PACKAGE + ".mapper",
    sqlSessionTemplateRef = SupervisorDataSourceConfig.SPRING_BEAN_NAME_SQL_SESSION_TEMPLATE
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
    private static final String DB_NAME = "disjob";

    /**
     * Spring bean name datasource
     */
    public static final String SPRING_BEAN_NAME_DATASOURCE = DB_NAME + DATA_SOURCE_NAME_SUFFIX;

    /**
     * Spring bean name transaction manager
     */
    public static final String SPRING_BEAN_NAME_TX_MANAGER = DB_NAME + TX_MANAGER_NAME_SUFFIX;

    /**
     * Spring bean name transaction template
     */
    public static final String SPRING_BEAN_NAME_TX_TEMPLATE = DB_NAME + TX_TEMPLATE_NAME_SUFFIX;

    /**
     * Spring bean name JDBC template
     */
    public static final String SPRING_BEAN_NAME_JDBC_TEMPLATE = DB_NAME + JDBC_TEMPLATE_NAME_SUFFIX;

    /**
     * Spring bean name mybatis sql session factory
     */
    public static final String SPRING_BEAN_NAME_SQL_SESSION_FACTORY = DB_NAME + SQL_SESSION_FACTORY_NAME_SUFFIX;

    /**
     * Spring bean name mybatis sql session template
     */
    public static final String SPRING_BEAN_NAME_SQL_SESSION_TEMPLATE = DB_NAME + SQL_SESSION_TEMPLATE_NAME_SUFFIX;

    @Bean(name = SPRING_BEAN_NAME_DATASOURCE)
    @ConfigurationProperties(prefix = DB_NAME + ".datasource")
    @Override
    public DataSource dataSource() {
        return DataSourceBuilder.create()
            //.type(com.zaxxer.hikari.HikariDataSource.class)
            .build();
    }

    @Bean(name = SPRING_BEAN_NAME_SQL_SESSION_FACTORY)
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        return super.createSqlSessionFactory();
    }

    @Bean(name = SPRING_BEAN_NAME_SQL_SESSION_TEMPLATE)
    public SqlSessionTemplate sqlSessionTemplate() throws Exception {
        return new SqlSessionTemplate(sqlSessionFactory());
    }

    @Bean(name = SPRING_BEAN_NAME_TX_MANAGER)
    public DataSourceTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource());
    }

    @Bean(name = SPRING_BEAN_NAME_TX_TEMPLATE)
    public TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(transactionManager());
    }

    @Bean(name = SPRING_BEAN_NAME_JDBC_TEMPLATE)
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource());
    }

}

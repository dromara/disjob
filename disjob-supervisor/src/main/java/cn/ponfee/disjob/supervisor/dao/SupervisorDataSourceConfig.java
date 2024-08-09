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

import cn.ponfee.disjob.common.spring.MybatisDataSourceConfigurer;
import org.springframework.context.annotation.Configuration;

/**
 * Supervisor datasource configuration
 *
 * <pre>
 *  # 前面的`disjob.datasource`为固定前缀，后面的`disjob`为数据源名
 *  disjob.datasource:
 *    disjob:
 *      driver-class-name: com.mysql.cj.jdbc.Driver
 *      jdbc-url: jdbc:mysql://localhost:3306/disjob?useUnicode=true&characterEncoding=UTF-8&useSSL=false&autoReconnect=true&connectTimeout=2000&socketTimeout=5000&serverTimezone=Asia/Shanghai&failOverReadOnly=false
 *      username: disjob
 *      password:
 *      minimum-idle: 10
 *      maximum-pool-size: 100
 *      connection-timeout: 2000
 * </pre>
 *
 * @author Ponfee
 */
@Configuration
@MybatisDataSourceConfigurer(
    dataSourceName = SupervisorDataSourceConfig.DATA_SOURCE_NAME,
    mapperLocations = "classpath*:cn/ponfee/disjob/supervisor/dao/xml/*.xml"
)
public class SupervisorDataSourceConfig {

    /**
     * Data source name
     */
    static final String DATA_SOURCE_NAME = "disjob";

    /**
     * Spring bean name transaction manager
     */
    public static final String SPRING_BEAN_NAME_TX_MANAGER = DATA_SOURCE_NAME + MybatisDataSourceConfigurer.TX_MANAGER_NAME_SUFFIX;

    /**
     * Spring bean name transaction template
     */
    public static final String SPRING_BEAN_NAME_TX_TEMPLATE = DATA_SOURCE_NAME + MybatisDataSourceConfigurer.TX_TEMPLATE_NAME_SUFFIX;

    /**
     * Spring bean name JDBC template
     */
    public static final String SPRING_BEAN_NAME_JDBC_TEMPLATE = DATA_SOURCE_NAME + MybatisDataSourceConfigurer.JDBC_TEMPLATE_NAME_SUFFIX;

}

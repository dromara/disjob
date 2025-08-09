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

package cn.ponfee.disjob.supervisor.disjob_admin;

import cn.ponfee.disjob.common.spring.MybatisDataSourceConfigurer;
import org.springframework.context.annotation.Configuration;

/**
 * Admin DataSource Config
 *
 * @author Ponfee
 */
@Configuration
@MybatisDataSourceConfigurer(
    mapperLocations = "classpath*:cn/ponfee/disjob/supervisor/disjob_admin/xml/*.xml",
    basePackages = {"cn.ponfee.disjob.supervisor.disjob_admin", "cn.ponfee.disjob.unknown"},
    basePackageClasses = UserMapperTest.class,
    typeAliasesPackage = "cn.ponfee.disjob.nonexistent",
    defaultFetchSize = 99
)
public class AdminDataSourceConfig /*extends cn.ponfee.disjob.common.spring.AbstractMybatisDataSourceConfig*/ {

    //public AdminDataSourceConfig() {
    //    super(DATA_SOURCE_NAME);
    //}

    /**
     * Data source name
     */
    static final String DATA_SOURCE_NAME = "disjob-admin";

    static {
        MybatisDataSourceConfigurer.MybatisDataSourceRegistrar.checkPackageDatasourceName(AdminDataSourceConfig.class, DATA_SOURCE_NAME);
    }

    /**
     * Spring bean name transaction manager
     */
    public static final String SPRING_BEAN_NAME_TX_MANAGER = DATA_SOURCE_NAME + MybatisDataSourceConfigurer.TX_MANAGER_NAME_SUFFIX;

    /**
     * Spring bean name transaction template
     */
    public static final String SPRING_BEAN_NAME_TX_TEMPLATE = DATA_SOURCE_NAME + MybatisDataSourceConfigurer.TX_TEMPLATE_NAME_SUFFIX;

}

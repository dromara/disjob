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

package cn.ponfee.disjob.admin;

import cn.ponfee.disjob.common.spring.JacksonDateConfigurer;
import cn.ponfee.disjob.id.snowflake.db.DbSnowflakeIdGenerator;
import cn.ponfee.disjob.supervisor.configuration.EnableSupervisor;
import cn.ponfee.disjob.worker.configuration.EnableWorker;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import static cn.ponfee.disjob.supervisor.dao.SupervisorDataSourceConfig.SPRING_BEAN_NAME_JDBC_TEMPLATE;

/**
 * Disjob admin configuration
 *
 * @author Ponfee
 */
@Configuration
@DbSnowflakeIdGenerator(jdbcTemplateRef = SPRING_BEAN_NAME_JDBC_TEMPLATE)
@ComponentScan("cn.ponfee.disjob.test.handler") // 加载一些测试的JobHandler，只用于demo演示使用(开发时建议删掉这行)
@JacksonDateConfigurer                          // 解决日期反序列化报错的问题
@EnableSupervisor                               // disjob-admin必须启用Supervisor角色，即：必须加@EnableSupervisor注解
@EnableWorker                                   // 若要取消worker角色可去掉@EnableWorker注解(生产建议Supervisor与Worker分开部署，即去掉@EnableWorker注解)
public class DisjobAdminConfiguration {

}

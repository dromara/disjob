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

package cn.ponfee.disjob.samples.merged;

import cn.ponfee.disjob.id.snowflake.db.DbSnowflakeIdGenerator;
import cn.ponfee.disjob.supervisor.configuration.EnableSupervisor;
import cn.ponfee.disjob.test.executor.SamplesJobExecutorPackage;
import cn.ponfee.disjob.worker.configuration.EnableWorker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

import static cn.ponfee.disjob.supervisor.dao.SupervisorDataSourceConfig.SPRING_BEAN_NAME_JDBC_TEMPLATE;

/**
 * Merged Supervisor and Worker application based spring boot
 *
 * @author Ponfee
 */
// scan cn.ponfee.disjob.test.executor package
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class, scanBasePackageClasses = SamplesJobExecutorPackage.class)
@DbSnowflakeIdGenerator(jdbcTemplateRef = SPRING_BEAN_NAME_JDBC_TEMPLATE)
@EnableSupervisor
@EnableWorker
//@de.codecentric.boot.admin.server.config.EnableAdminServer
public class MergedApplication {

    static {
        // for log4j2 log file name
        System.setProperty("app.name", "springboot-merged");
    }

    public static void main(String[] args) {
        SpringApplication.run(MergedApplication.class, args);
    }

}

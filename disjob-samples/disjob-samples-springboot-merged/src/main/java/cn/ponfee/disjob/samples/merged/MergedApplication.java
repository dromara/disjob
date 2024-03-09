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

import cn.ponfee.disjob.common.base.IdGenerator;
import cn.ponfee.disjob.common.base.Symbol.Char;
import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.util.JobUtils;
import cn.ponfee.disjob.id.snowflake.db.DbDistributedSnowflake;
import cn.ponfee.disjob.samples.common.AbstractSamplesApplication;
import cn.ponfee.disjob.supervisor.configuration.EnableSupervisor;
import cn.ponfee.disjob.worker.configuration.EnableWorker;
import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import static cn.ponfee.disjob.supervisor.dao.SupervisorDataSourceConfig.SPRING_BEAN_NAME_JDBC_TEMPLATE;

/**
 * Disjob application based spring boot
 *
 * @author Ponfee
 */
@EnableSupervisor
@EnableWorker
@EnableAdminServer
public class MergedApplication extends AbstractSamplesApplication {

    static {
        // for log4j2 log file name
        System.setProperty("app.name", "springboot-merged");
    }

    public static void main(String[] args) {
        SpringApplication.run(MergedApplication.class, args);
    }

    @Bean
    public IdGenerator idGenerator(@Qualifier(SPRING_BEAN_NAME_JDBC_TEMPLATE) JdbcTemplate jdbcTemplate,
                                   @Value("${" + JobConstants.SPRING_WEB_SERVER_PORT + "}") int port,
                                   @Value("${" + JobConstants.DISJOB_BOUND_SERVER_HOST + ":}") String boundHost) {
        return new DbDistributedSnowflake(jdbcTemplate, JobConstants.DISJOB_KEY_PREFIX, JobUtils.getLocalHost(boundHost) + Char.COLON + port);
    }

}

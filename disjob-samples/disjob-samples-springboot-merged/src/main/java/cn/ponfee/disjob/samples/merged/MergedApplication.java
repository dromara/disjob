/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

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

import static cn.ponfee.disjob.supervisor.base.AbstractDataSourceConfig.JDBC_TEMPLATE_NAME_SUFFIX;
import static cn.ponfee.disjob.supervisor.dao.SupervisorDataSourceConfig.DB_NAME;

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
        // for log4j log file dir
        System.setProperty("app.name", "springboot-merged");
    }

    public static void main(String[] args) {
        SpringApplication.run(MergedApplication.class, args);
    }

    @Bean
    public IdGenerator idGenerator(@Qualifier(DB_NAME + JDBC_TEMPLATE_NAME_SUFFIX) JdbcTemplate jdbcTemplate,
                                   @Value("${" + JobConstants.SPRING_WEB_SERVER_PORT + "}") int port,
                                   @Value("${" + JobConstants.DISJOB_BOUND_SERVER_HOST + ":}") String boundHost) {
        return new DbDistributedSnowflake(jdbcTemplate, JobConstants.DISJOB_KEY_PREFIX, JobUtils.getLocalHost(boundHost) + Char.COLON + port);
    }

}

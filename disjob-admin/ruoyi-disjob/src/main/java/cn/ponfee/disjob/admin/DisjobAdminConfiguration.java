/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.admin;

import cn.ponfee.disjob.common.base.IdGenerator;
import cn.ponfee.disjob.common.base.Symbol.Char;
import cn.ponfee.disjob.common.spring.EnableJacksonDateConfigurer;
import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.util.JobUtils;
import cn.ponfee.disjob.id.snowflake.db.DbDistributedSnowflake;
import cn.ponfee.disjob.supervisor.configuration.EnableSupervisor;
import cn.ponfee.disjob.worker.configuration.EnableWorker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import static cn.ponfee.disjob.supervisor.dao.SupervisorDataSourceConfig.JDBC_TEMPLATE_SPRING_BEAN_NAME;

/**
 * Disjob admin configuration
 *
 * @author Ponfee
 */
@Configuration
@ComponentScan("cn.ponfee.disjob.test.handler")
@EnableJacksonDateConfigurer // 解决日期反序列化报错的问题
@EnableSupervisor            // disjob-admin必须启用Supervisor角色，即：必须加@EnableSupervisor注解
@EnableWorker                // 若要取消worker角色可去掉@EnableWorker注解
public class DisjobAdminConfiguration {

    @Bean
    public IdGenerator idGenerator(@Qualifier(JDBC_TEMPLATE_SPRING_BEAN_NAME) JdbcTemplate jdbcTemplate,
                                   @Value("${" + JobConstants.SPRING_WEB_SERVER_PORT + "}") int port,
                                   @Value("${" + JobConstants.DISJOB_BOUND_SERVER_HOST + ":}") String boundHost) {
        // serverTag = host:port
        String serverTag = JobUtils.getLocalHost(boundHost) + Char.COLON + port;
        return new DbDistributedSnowflake(jdbcTemplate, JobConstants.DISJOB_KEY_PREFIX, serverTag);
    }

}

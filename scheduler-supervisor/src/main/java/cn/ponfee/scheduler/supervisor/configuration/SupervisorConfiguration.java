package cn.ponfee.scheduler.supervisor.configuration;

import cn.ponfee.scheduler.common.base.Constants;
import cn.ponfee.scheduler.common.lock.DoInDatabaseLocked;
import cn.ponfee.scheduler.common.lock.DoInLocked;
import cn.ponfee.scheduler.common.spring.SpringContextHolder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import static cn.ponfee.scheduler.supervisor.base.SupervisorConstants.*;
import static cn.ponfee.scheduler.supervisor.dao.SchedulerDataSourceConfig.DB_NAME;

/**
 * Core configuration.
 *
 * @author Ponfee
 */
@Configuration
public class SupervisorConfiguration {

    @Bean
    public SpringContextHolder springContextHolder() {
        return new SpringContextHolder();
    }

    @Bean(SPRING_BEAN_NAME_SCAN_JOB_LOCKED)
    public DoInLocked scanJobLocked(@Qualifier(DB_NAME + Constants.JDBC_TEMPLATE_SUFFIX) JdbcTemplate jdbcTemplate) {
        return new DoInDatabaseLocked(jdbcTemplate, LOCK_SQL_SCAN_JOB);
    }

    @Bean(SPRING_BEAN_NAME_SCAN_TRACK_LOCKED)
    public DoInLocked scanTrackLocked(@Qualifier(DB_NAME + Constants.JDBC_TEMPLATE_SUFFIX) JdbcTemplate jdbcTemplate) {
        return new DoInDatabaseLocked(jdbcTemplate, LOCK_SQL_SCAN_TRACK);
    }

}

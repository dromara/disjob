/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.configuration;

import cn.ponfee.scheduler.common.base.Constants;
import cn.ponfee.scheduler.common.lock.DoInDatabaseLocked;
import cn.ponfee.scheduler.common.lock.DoInLocked;
import cn.ponfee.scheduler.common.spring.SpringContextHolder;
import cn.ponfee.scheduler.common.util.ClassUtils;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.common.util.Networks;
import cn.ponfee.scheduler.core.base.*;
import cn.ponfee.scheduler.registry.DiscoveryRestProxy;
import cn.ponfee.scheduler.registry.DiscoveryRestTemplate;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import cn.ponfee.scheduler.supervisor.SupervisorStartup;
import cn.ponfee.scheduler.supervisor.base.SupervisorConstants;
import cn.ponfee.scheduler.supervisor.base.WorkerServiceClient;
import cn.ponfee.scheduler.supervisor.dao.SchedulerDataSourceConfig;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;

import java.lang.annotation.*;

/**
 * Enable supervisor role
 * <p>必须注解到具有@Component的类上且该类能被spring扫描到
 *
 * <pre>
 * @Order、Order接口、@AutoConfigureBefore、@AutoConfigureAfter、@AutoConfigureOrder的顺序：
 *   1）用户自定义的类之间的顺序是按照文件的目录结构从上到下排序且无法干预，在这里这些方式都是无效的；
 *   2）自动装配的类之间可以使用这五种方式去改变加载的顺序（用户自定义的类 排在 EnableAutoConfiguration自动配置加载的类 的前面）；
 * </pre>
 *
 * @author Ponfee
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@EnableConfigurationProperties(SupervisorProperties.class)
@Import({
    EnableSupervisor.EnableComponentScan.class,
    EnableSupervisor.EnableHttpProperties.class,
    EnableSupervisor.EnableSupervisorConfiguration.class,
    SupervisorStartupRunner.class,
})
public @interface EnableSupervisor {

    @ComponentScan(basePackageClasses = SupervisorStartup.class)
    class EnableComponentScan {
    }

    @ConditionalOnMissingBean(HttpProperties.class)
    @EnableConfigurationProperties(HttpProperties.class)
    class EnableHttpProperties {
    }

    @ConditionalOnProperty(JobConstants.SPRING_WEB_SERVER_PORT)
    class EnableSupervisorConfiguration {
        @AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
        @Order(Ordered.HIGHEST_PRECEDENCE)
        @ConditionalOnMissingBean
        @Bean(JobConstants.SPRING_BEAN_NAME_CURRENT_SUPERVISOR)
        public Supervisor currentSupervisor(@Value("${" + JobConstants.SPRING_WEB_SERVER_PORT + "}") int port) {
            Supervisor currentSupervisor = new Supervisor(Networks.getHostIp(), port);
            // inject current supervisor: Supervisor.class.getDeclaredClasses()[0]
            try {
                ClassUtils.invoke(Class.forName(Supervisor.class.getName() + "$Current"), "set", new Object[]{currentSupervisor});
            } catch (ClassNotFoundException e) {
                // cannot happen
                throw new AssertionError("Setting as current supervisor occur error.", e);
            }
            return currentSupervisor;
        }

        @DependsOn(JobConstants.SPRING_BEAN_NAME_CURRENT_SUPERVISOR)
        @ConditionalOnMissingBean
        @Bean
        public WorkerServiceClient workerServiceClient(HttpProperties properties,
                                                       SupervisorRegistry supervisorRegistry,
                                                       @Nullable Worker currentWorker,
                                                       @Nullable @Qualifier(JobConstants.SPRING_BEAN_NAME_OBJECT_MAPPER) ObjectMapper objectMapper) {
            DiscoveryRestTemplate<Worker> discoveryRestTemplate = DiscoveryRestTemplate.<Worker>builder()
                .connectTimeout(properties.getConnectTimeout())
                .readTimeout(properties.getReadTimeout())
                .maxRetryTimes(properties.getMaxRetryTimes())
                .objectMapper(objectMapper != null ? objectMapper : Jsons.createObjectMapper(JsonInclude.Include.NON_NULL))
                .discoveryServer(supervisorRegistry)
                .build();
            WorkerService remoteWorkerService = DiscoveryRestProxy.create(WorkerService.class, discoveryRestTemplate);
            return new WorkerServiceClient(currentWorker, remoteWorkerService);
        }

        @Bean(SupervisorConstants.SPRING_BEAN_NAME_SCAN_JOB_LOCKED)
        public DoInLocked scanJobLocked(@Qualifier(SchedulerDataSourceConfig.DB_NAME + Constants.JDBC_TEMPLATE_SUFFIX) JdbcTemplate jdbcTemplate) {
            return new DoInDatabaseLocked(jdbcTemplate, SupervisorConstants.LOCK_SQL_SCAN_JOB);
        }

        @Bean(SupervisorConstants.SPRING_BEAN_NAME_SCAN_TRACK_LOCKED)
        public DoInLocked scanTrackLocked(@Qualifier(SchedulerDataSourceConfig.DB_NAME + Constants.JDBC_TEMPLATE_SUFFIX) JdbcTemplate jdbcTemplate) {
            return new DoInDatabaseLocked(jdbcTemplate, SupervisorConstants.LOCK_SQL_SCAN_TRACK);
        }

        @ConditionalOnMissingBean
        @Bean
        public SpringContextHolder springContextHolder() {
            return new SpringContextHolder();
        }
    }

}

/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.configuration;

import cn.ponfee.disjob.common.lock.DoInDatabaseLocked;
import cn.ponfee.disjob.common.lock.DoInLocked;
import cn.ponfee.disjob.common.spring.SpringContextHolder;
import cn.ponfee.disjob.common.util.ClassUtils;
import cn.ponfee.disjob.core.base.*;
import cn.ponfee.disjob.core.util.JobUtils;
import cn.ponfee.disjob.registry.DiscoveryRestProxy;
import cn.ponfee.disjob.registry.DiscoveryRestTemplate;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.supervisor.SupervisorStartup;
import cn.ponfee.disjob.supervisor.base.AbstractDataSourceConfig;
import cn.ponfee.disjob.supervisor.base.SupervisorConstants;
import cn.ponfee.disjob.supervisor.base.WorkerServiceClient;
import cn.ponfee.disjob.supervisor.dao.SupervisorDataSourceConfig;
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
    EnableSupervisor.EnableRetryProperties.class,
    EnableSupervisor.EnableHttpProperties.class,
    EnableSupervisor.EnableSupervisorConfiguration.class,
    EnableSupervisor.EnableComponentScan.class,
    SupervisorLifecycle.class
})
public @interface EnableSupervisor {

    @ConditionalOnMissingBean(RetryProperties.class)
    @EnableConfigurationProperties(RetryProperties.class)
    class EnableRetryProperties {
    }

    @ConditionalOnMissingBean(HttpProperties.class)
    @EnableConfigurationProperties(HttpProperties.class)
    class EnableHttpProperties {
    }

    @ComponentScan(basePackageClasses = SupervisorStartup.class)
    class EnableComponentScan {
    }

    @ConditionalOnProperty(JobConstants.SPRING_WEB_SERVER_PORT)
    class EnableSupervisorConfiguration {

        @AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
        @Order(Ordered.HIGHEST_PRECEDENCE)
        @ConditionalOnMissingBean
        @Bean(JobConstants.SPRING_BEAN_NAME_CURRENT_SUPERVISOR)
        public Supervisor currentSupervisor(@Value("${" + JobConstants.SPRING_WEB_SERVER_PORT + "}") int port,
                                            @Value("${" + JobConstants.DISJOB_BOUND_SERVER_HOST + ":}") String boundHost) {
            String host = JobUtils.getLocalHost(boundHost);
            Supervisor currentSupervisor = new Supervisor(host, port);
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
        public WorkerServiceClient workerServiceClient(HttpProperties httpProperties,
                                                       RetryProperties retryProperties,
                                                       SupervisorRegistry supervisorRegistry,
                                                       @Nullable Worker currentWorker,
                                                       @Nullable ObjectMapper objectMapper) {
            httpProperties.check();
            retryProperties.check();
            DiscoveryRestTemplate<Worker> discoveryRestTemplate = DiscoveryRestTemplate.<Worker>builder()
                .httpConnectTimeout(httpProperties.getConnectTimeout())
                .httpReadTimeout(httpProperties.getReadTimeout())
                .retryMaxCount(retryProperties.getMaxCount())
                .retryBackoffPeriod(retryProperties.getBackoffPeriod())
                .objectMapper(objectMapper)
                .discoveryServer(supervisorRegistry)
                .build();
            WorkerService remoteWorkerService = DiscoveryRestProxy.create(true, WorkerService.class, discoveryRestTemplate);
            return new WorkerServiceClient(remoteWorkerService, currentWorker);
        }

        @Bean(SupervisorConstants.SPRING_BEAN_NAME_SCAN_TRIGGERING_JOB_LOCKER)
        public DoInLocked scanTriggeringJobLocker(@Qualifier(SupervisorDataSourceConfig.DB_NAME + AbstractDataSourceConfig.JDBC_TEMPLATE_NAME_SUFFIX) JdbcTemplate jdbcTemplate) {
            return new DoInDatabaseLocked(jdbcTemplate, SupervisorConstants.LOCK_SQL_SCAN_TRIGGERING_JOB);
        }

        @Bean(SupervisorConstants.SPRING_BEAN_NAME_SCAN_WAITING_INSTANCE_LOCKER)
        public DoInLocked scanWaitingInstanceLocker(@Qualifier(SupervisorDataSourceConfig.DB_NAME + AbstractDataSourceConfig.JDBC_TEMPLATE_NAME_SUFFIX) JdbcTemplate jdbcTemplate) {
            return new DoInDatabaseLocked(jdbcTemplate, SupervisorConstants.LOCK_SQL_SCAN_WAITING_INSTANCE);
        }

        @Bean(SupervisorConstants.SPRING_BEAN_NAME_SCAN_RUNNING_INSTANCE_LOCKER)
        public DoInLocked scanRunningInstanceLocker(@Qualifier(SupervisorDataSourceConfig.DB_NAME + AbstractDataSourceConfig.JDBC_TEMPLATE_NAME_SUFFIX) JdbcTemplate jdbcTemplate) {
            return new DoInDatabaseLocked(jdbcTemplate, SupervisorConstants.LOCK_SQL_SCAN_RUNNING_INSTANCE);
        }

        @ConditionalOnMissingBean
        @Bean
        public SpringContextHolder springContextHolder() {
            return new SpringContextHolder();
        }
    }

}

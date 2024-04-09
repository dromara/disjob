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

package cn.ponfee.disjob.supervisor.configuration;

import cn.ponfee.disjob.common.lock.DoInDatabaseLocked;
import cn.ponfee.disjob.common.lock.DoInLocked;
import cn.ponfee.disjob.common.spring.LocalizedMethodArgumentConfigurer;
import cn.ponfee.disjob.common.spring.RestTemplateUtils;
import cn.ponfee.disjob.common.spring.SpringContextHolder;
import cn.ponfee.disjob.common.spring.SpringUtils;
import cn.ponfee.disjob.common.util.ClassUtils;
import cn.ponfee.disjob.core.base.*;
import cn.ponfee.disjob.core.util.JobUtils;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.registry.rpc.DestinationServerRestProxy;
import cn.ponfee.disjob.registry.rpc.DestinationServerRestProxy.DestinationServerInvoker;
import cn.ponfee.disjob.registry.rpc.DiscoveryServerRestProxy;
import cn.ponfee.disjob.registry.rpc.DiscoveryServerRestProxy.GroupedServerInvoker;
import cn.ponfee.disjob.supervisor.SupervisorStartup;
import cn.ponfee.disjob.supervisor.application.SchedGroupService;
import cn.ponfee.disjob.supervisor.auth.AuthenticationConfigurer;
import cn.ponfee.disjob.supervisor.base.SupervisorConstants;
import cn.ponfee.disjob.supervisor.component.DistributedJobManager;
import cn.ponfee.disjob.supervisor.component.DistributedJobQuerier;
import cn.ponfee.disjob.supervisor.provider.SupervisorRpcProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RestTemplate;

import java.lang.annotation.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static cn.ponfee.disjob.supervisor.dao.SupervisorDataSourceConfig.SPRING_BEAN_NAME_JDBC_TEMPLATE;

/**
 * Enable supervisor role
 * <p>必须注解到具有@Component的类上且该类能被spring扫描到
 *
 * <pre>
 * `@Order、Order接口、@AutoConfigureBefore、@AutoConfigureAfter、@AutoConfigureOrder的顺序：
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
    EnableSupervisor.EnableScanLockerConfiguration.class,
    EnableSupervisor.EnableSupervisorAdapter.class,
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

    class EnableSupervisorConfiguration {

        @Bean(JobConstants.SPRING_BEAN_NAME_CURRENT_SUPERVISOR)
        public Supervisor.Current currentSupervisor(WebServerApplicationContext webServerApplicationContext,
                                                    @Value("${" + JobConstants.DISJOB_BOUND_SERVER_HOST + ":}") String boundHost) {
            UnaryOperator<String> workerCtxPath = group -> SchedGroupService.getGroup(group).getWorkerContextPath();
            String host = JobUtils.getLocalHost(boundHost);
            int port = SpringUtils.getActualWebServerPort(webServerApplicationContext);
            Object[] args = {host, port, workerCtxPath};
            try {
                // inject current supervisor: Supervisor.class.getDeclaredClasses()[0]
                return ClassUtils.invoke(Class.forName(Supervisor.Current.class.getName()), "create", args);
            } catch (ClassNotFoundException e) {
                // cannot happen
                throw new Error("Setting as current supervisor occur error.", e);
            }
        }

        @ConditionalOnMissingBean
        @Bean(JobConstants.SPRING_BEAN_NAME_REST_TEMPLATE)
        public RestTemplate restTemplate(HttpProperties http, @Nullable ObjectMapper objectMapper) {
            http.check();
            return RestTemplateUtils.create(http.getConnectTimeout(), http.getReadTimeout(), objectMapper);
        }

        @DependsOn(JobConstants.SPRING_BEAN_NAME_CURRENT_SUPERVISOR)
        @Bean
        public GroupedServerInvoker<WorkerRpcService> groupedWorkerRpcClient(RetryProperties retry,
                                                                             @Qualifier(JobConstants.SPRING_BEAN_NAME_REST_TEMPLATE) RestTemplate restTemplate,
                                                                             SupervisorRegistry discoveryWorker,
                                                                             @Nullable WorkerRpcService workerRpcProvider,
                                                                             @Nullable Worker.Current currentWorker) {
            retry.check();
            Predicate<String> serverGroupMatcher = group -> Worker.matchesGroup(currentWorker, group);
            return DiscoveryServerRestProxy.create(
                WorkerRpcService.class, workerRpcProvider, serverGroupMatcher, discoveryWorker, restTemplate, retry
            );
        }

        @DependsOn(JobConstants.SPRING_BEAN_NAME_CURRENT_SUPERVISOR)
        @Bean
        public DestinationServerInvoker<WorkerRpcService, Worker> destinationWorkerRpcClient(@Qualifier(JobConstants.SPRING_BEAN_NAME_REST_TEMPLATE) RestTemplate restTemplate,
                                                                                             @Nullable WorkerRpcService workerRpcProvider) {
            RetryProperties retry = RetryProperties.of(0, 0);
            Function<Worker, String> workerContextPath = worker -> Supervisor.current().getWorkerContextPath(worker.getGroup());
            return DestinationServerRestProxy.create(
                WorkerRpcService.class, workerRpcProvider, Worker.current(), workerContextPath, restTemplate, retry
            );
        }

        @Bean
        public AuthenticationConfigurer authenticationConfigurer() {
            return new AuthenticationConfigurer();
        }

        // 如果注解没有参数，则默认以方法的返回类型判断，即容器中不存在类型为`LocalizedMethodArgumentConfigurer`的实例才创建
        @ConditionalOnMissingBean
        @Bean
        public LocalizedMethodArgumentConfigurer localizedMethodArgumentConfigurer() {
            return new LocalizedMethodArgumentConfigurer();
        }

        @ConditionalOnMissingBean
        @Bean
        public SpringContextHolder springContextHolder() {
            return new SpringContextHolder();
        }
    }

    @ComponentScan(basePackageClasses = SupervisorStartup.class)
    class EnableComponentScan {
    }

    @Order
    @ConditionalOnProperty(prefix = SupervisorProperties.KEY_PREFIX, name = "locker", havingValue = "default", matchIfMissing = true)
    class EnableScanLockerConfiguration {

        @ConditionalOnMissingBean(name = SupervisorConstants.SPRING_BEAN_NAME_SCAN_TRIGGERING_JOB_LOCKER)
        @Bean(SupervisorConstants.SPRING_BEAN_NAME_SCAN_TRIGGERING_JOB_LOCKER)
        public DoInLocked scanTriggeringJobLocker(@Qualifier(SPRING_BEAN_NAME_JDBC_TEMPLATE) JdbcTemplate jdbcTemplate) {
            return new DoInDatabaseLocked(jdbcTemplate, SupervisorConstants.LOCK_SCAN_TRIGGERING_JOB);
        }

        @ConditionalOnMissingBean(name = SupervisorConstants.SPRING_BEAN_NAME_SCAN_WAITING_INSTANCE_LOCKER)
        @Bean(SupervisorConstants.SPRING_BEAN_NAME_SCAN_WAITING_INSTANCE_LOCKER)
        public DoInLocked scanWaitingInstanceLocker(@Qualifier(SPRING_BEAN_NAME_JDBC_TEMPLATE) JdbcTemplate jdbcTemplate) {
            return new DoInDatabaseLocked(jdbcTemplate, SupervisorConstants.LOCK_SCAN_WAITING_INSTANCE);
        }

        @ConditionalOnMissingBean(name = SupervisorConstants.SPRING_BEAN_NAME_SCAN_RUNNING_INSTANCE_LOCKER)
        @Bean(SupervisorConstants.SPRING_BEAN_NAME_SCAN_RUNNING_INSTANCE_LOCKER)
        public DoInLocked scanRunningInstanceLocker(@Qualifier(SPRING_BEAN_NAME_JDBC_TEMPLATE) JdbcTemplate jdbcTemplate) {
            return new DoInDatabaseLocked(jdbcTemplate, SupervisorConstants.LOCK_SCAN_RUNNING_INSTANCE);
        }
    }

    class EnableSupervisorAdapter {

        @DependsOn(JobConstants.SPRING_BEAN_NAME_CURRENT_SUPERVISOR)
        @Bean
        public SupervisorRpcService supervisorRpcService(DistributedJobManager jobManager,
                                                         DistributedJobQuerier jobQuerier) {
            return new SupervisorRpcProvider(jobManager, jobQuerier);
        }
    }

}

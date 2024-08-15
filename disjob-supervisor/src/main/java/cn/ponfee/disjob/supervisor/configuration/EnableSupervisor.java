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

import cn.ponfee.disjob.common.spring.SpringUtils;
import cn.ponfee.disjob.common.util.ClassUtils;
import cn.ponfee.disjob.core.base.*;
import cn.ponfee.disjob.core.util.DisjobUtils;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.registry.rpc.DestinationServerRestProxy;
import cn.ponfee.disjob.registry.rpc.DestinationServerRestProxy.DestinationServerInvoker;
import cn.ponfee.disjob.registry.rpc.DiscoveryServerRestProxy;
import cn.ponfee.disjob.registry.rpc.DiscoveryServerRestProxy.GroupedServerInvoker;
import cn.ponfee.disjob.supervisor.SupervisorStartup;
import cn.ponfee.disjob.supervisor.application.SchedGroupService;
import cn.ponfee.disjob.supervisor.configuration.EnableSupervisor.EnableSupervisorConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RestTemplate;

import java.lang.annotation.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static cn.ponfee.disjob.core.base.JobConstants.SPRING_BEAN_NAME_LOCAL_SUPERVISOR;
import static cn.ponfee.disjob.core.base.JobConstants.SPRING_BEAN_NAME_REST_TEMPLATE;

/**
 * Enable supervisor role
 * <p>必须注解到具有@Component的类上且该类能被spring扫描到
 *
 * <pre>
 * `@AutoConfigureBefore、@AutoConfigureAfter、@AutoConfigureOrder：
 *   1）专门用于控制Spring-boot自动配置类之间的加载顺序（用户自定义的类是排在自动配置类`EnableAutoConfiguration`的前面）
 *   2）Spring-boot`2.7.x`配置文件位置：`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
 *
 * `@Order、@Priority、PriorityOrdered、Ordered
 *   1）用户自定义的类之间的创建顺序是按照文件的目录结构从上到下排序且无法干预
 *   2）影响List<Bean>元素的顺序，间接影响列表中bean的方法执行顺序，本质上是beanList.sort(AnnotationAwareOrderComparator.INSTANCE)
 *   3）`@Priority/PriorityOrdered`优先于`@Order/Ordered`
 *
 * `@Import(DisjobCoreDeferredImportSelector.class)
 *   1）多处@Import(XXX.class)同一个类，只会有一个生效，后面的会被去重忽略
 *   2）如果一个类即有@Component注解，又被@Import引入，则@Component生效，@Import被忽略
 * </pre>
 *
 * @author Ponfee
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({
    EnableSupervisorConfiguration.class,
    BasicDeferredImportSelector.class,
    SupervisorDeferredImportSelector.class,
    SupervisorLifecycle.class
})
public @interface EnableSupervisor {

    @EnableConfigurationProperties(SupervisorProperties.class)
    @ComponentScan(basePackageClasses = SupervisorStartup.class)
    class EnableSupervisorConfiguration {

        @Bean(SPRING_BEAN_NAME_LOCAL_SUPERVISOR)
        public Supervisor.Local localSupervisor(WebServerApplicationContext webServerApplicationContext) {
            UnaryOperator<String> workerCtxPath = group -> SchedGroupService.getGroup(group).getWorkerContextPath();
            String host = DisjobUtils.getLocalHost();
            int port = SpringUtils.getActualWebServerPort(webServerApplicationContext);
            Object[] args = {host, port, workerCtxPath};
            try {
                // inject local supervisor: Supervisor.class.getDeclaredClasses()[0]
                return ClassUtils.invoke(Class.forName(Supervisor.Local.class.getName()), "create", args);
            } catch (Exception e) {
                // cannot happen
                throw new Error("Creates Supervisor.Local instance occur error.", e);
            }
        }

        @DependsOn(SPRING_BEAN_NAME_LOCAL_SUPERVISOR)
        @Bean
        public GroupedServerInvoker<WorkerRpcService> groupedWorkerRpcClient(RetryProperties retry,
                                                                             SupervisorRegistry discoveryWorker,
                                                                             @Qualifier(SPRING_BEAN_NAME_REST_TEMPLATE) RestTemplate restTemplate,
                                                                             @Nullable WorkerRpcService workerRpcProvider,
                                                                             @Nullable Worker.Local localWorker) {
            retry.check();
            Predicate<String> serverGroupMatcher = localWorker != null ? localWorker::equalsGroup : g -> false;
            return DiscoveryServerRestProxy.create(
                WorkerRpcService.class, workerRpcProvider, serverGroupMatcher, discoveryWorker, restTemplate, retry
            );
        }

        @DependsOn(SPRING_BEAN_NAME_LOCAL_SUPERVISOR)
        @Bean
        public DestinationServerInvoker<WorkerRpcService, Worker> destinationWorkerRpcClient(@Qualifier(SPRING_BEAN_NAME_REST_TEMPLATE) RestTemplate restTemplate,
                                                                                             @Nullable WorkerRpcService workerRpcProvider) {
            RetryProperties retry = RetryProperties.of(0, 0);
            Function<Worker, String> workerContextPath = worker -> Supervisor.local().getWorkerContextPath(worker.getGroup());
            return DestinationServerRestProxy.create(
                WorkerRpcService.class, workerRpcProvider, Worker.local(), workerContextPath, restTemplate, retry
            );
        }
    }

}

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

package cn.ponfee.disjob.worker.configuration;

import cn.ponfee.disjob.common.spring.SpringUtils;
import cn.ponfee.disjob.common.util.ClassUtils;
import cn.ponfee.disjob.common.util.UuidUtils;
import cn.ponfee.disjob.core.base.DisjobCoreDeferredImportSelector;
import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.base.WorkerRpcService;
import cn.ponfee.disjob.core.util.DisjobUtils;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.worker.base.TaskTimingWheel;
import cn.ponfee.disjob.worker.configuration.EnableWorker.EnableWorkerConfiguration;
import cn.ponfee.disjob.worker.provider.WorkerRpcProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enable worker role
 * <p>必须注解到具有@Component的类上且该类能被spring扫描到
 *
 * @author Ponfee
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({
    EnableWorkerConfiguration.class,
    DisjobCoreDeferredImportSelector.class,
    WorkerLifecycle.class,
})
public @interface EnableWorker {

    @EnableConfigurationProperties(WorkerProperties.class)
    class EnableWorkerConfiguration {

        @Bean(JobConstants.SPRING_BEAN_NAME_CURRENT_WORKER)
        public Worker.Current currentWorker(WebServerApplicationContext webServerApplicationContext,
                                            WorkerProperties config) {
            config.check();
            String host = DisjobUtils.getLocalHost();
            String workerToken = config.getWorkerToken();
            String supervisorToken = config.getSupervisorToken();
            String supervisorContextPath = config.getSupervisorContextPath();
            int port = SpringUtils.getActualWebServerPort(webServerApplicationContext);

            Object[] args = {config.getGroup(), UuidUtils.uuid32(), host, port, workerToken, supervisorToken, supervisorContextPath};
            try {
                // inject current worker: Worker.class.getDeclaredClasses()[0]
                return ClassUtils.invoke(Class.forName(Worker.Current.class.getName()), "create", args);
            } catch (Exception e) {
                // cannot happen
                throw new Error("Creates Worker.Current instance occur error.", e);
            }
        }

        @Bean(JobConstants.SPRING_BEAN_NAME_TIMING_WHEEL)
        public TaskTimingWheel timingWheel(Worker.Current currentWorker, WorkerProperties config) {
            return new TaskTimingWheel(currentWorker, config.getTimingWheelTickMs(), config.getTimingWheelRingSize());
        }

        @Bean
        public WorkerRpcService workerRpcService(Worker.Current currentWorker, WorkerRegistry workerRegistry) {
            return new WorkerRpcProvider(currentWorker, workerRegistry);
        }
    }

}

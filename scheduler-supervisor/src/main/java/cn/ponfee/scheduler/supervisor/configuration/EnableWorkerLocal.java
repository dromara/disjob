package cn.ponfee.scheduler.supervisor.configuration;

import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.WorkerLocal;
import cn.ponfee.scheduler.core.base.WorkerService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enable worker service local implementation.
 *
 * @author Ponfee
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(EnableWorkerLocal.WorkerLocalConfiguration.class)
public @interface EnableWorkerLocal {

    @ConditionalOnClass({WorkerService.class})
    class WorkerLocalConfiguration {
        @Bean(JobConstants.SPRING_BEAN_NAME_WORKER_CLIENT)
        @ConditionalOnMissingBean
        public WorkerService workerClient() {
            return new WorkerLocal();
        }
    }

}

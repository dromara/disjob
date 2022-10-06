package cn.ponfee.scheduler.worker.configuration;

import cn.ponfee.scheduler.common.util.ClassUtils;
import cn.ponfee.scheduler.common.util.Networks;
import cn.ponfee.scheduler.common.util.ObjectUtils;
import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.Worker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.annotation.*;

/**
 * Enable worker role
 *
 * @author Ponfee
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(EnableWorker.CurrentWorkerConfiguration.class)
public @interface EnableWorker {

    @ConditionalOnClass({Worker.class})
    @ConditionalOnProperty(JobConstants.SPRING_WEB_SERVER_PORT)
    @AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
    class CurrentWorkerConfiguration {
        @Bean(JobConstants.SPRING_BEAN_NAME_CURRENT_WORKER)
        @Order(Ordered.HIGHEST_PRECEDENCE)
        @ConditionalOnMissingBean
        public Worker currentWorker(@Value("${" + JobConstants.SPRING_WEB_SERVER_PORT + "}") int port,
                                    @Value("${" + JobConstants.WORKER_KEY_PREFIX + ".group:default}") String group) {
            Worker currentWorker = new Worker(group, ObjectUtils.uuid32(), Networks.getHostIp(), port);
            // inject current worker: Worker.class.getDeclaredClasses()[0]
            try {
                ClassUtils.invoke(Class.forName(Worker.class.getName() + "$Current"), "set", new Object[]{currentWorker});
            } catch (ClassNotFoundException e) {
                // cannot happen
                throw new AssertionError("Setting as current worker occur error.", e);
            }
            return currentWorker;
        }
    }

}

/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.worker.configuration;

import cn.ponfee.scheduler.common.util.ClassUtils;
import cn.ponfee.scheduler.common.util.Networks;
import cn.ponfee.scheduler.common.util.ObjectUtils;
import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.worker.base.TaskTimingWheel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
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
@Import({EnableWorker.CurrentWorkerConfiguration.class, EnableWorker.TimingWheelConfiguration.class})
public @interface EnableWorker {

    @ConditionalOnClass({Worker.class})
    @ConditionalOnProperty(JobConstants.SPRING_WEB_SERVER_PORT)
    @AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
    class CurrentWorkerConfiguration {
        @Bean(JobConstants.SPRING_BEAN_NAME_CURRENT_WORKER)
        @Order(Ordered.HIGHEST_PRECEDENCE)
        @ConditionalOnMissingBean
        public Worker currentWorker(@Value("${" + JobConstants.SPRING_WEB_SERVER_PORT + "}") int port,
                                    WorkerProperties config) {
            Worker currentWorker = new Worker(config.getGroup(), ObjectUtils.uuid32(), Networks.getHostIp(), port);
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

    @ConditionalOnBean({Worker.class})
    @DependsOn(JobConstants.SPRING_BEAN_NAME_CURRENT_WORKER)
    class TimingWheelConfiguration {
        @Bean(JobConstants.SPRING_BEAN_NAME_TIMING_WHEEL)
        @ConditionalOnMissingBean
        public TaskTimingWheel timingWheel(WorkerProperties config) {
            return new TaskTimingWheel(config.getTickMs(), config.getRingSize());
        }
    }

}

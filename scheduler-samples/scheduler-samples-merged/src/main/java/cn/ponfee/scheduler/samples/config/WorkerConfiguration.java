package cn.ponfee.scheduler.samples.config;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.worker.base.TaskTimingWheel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Worker configuration
 *
 * @author Ponfee
 */
@Configuration
public class WorkerConfiguration {

    @Bean
    public TimingWheel<ExecuteParam> timingWheel() {
        return new TaskTimingWheel();
    }

}

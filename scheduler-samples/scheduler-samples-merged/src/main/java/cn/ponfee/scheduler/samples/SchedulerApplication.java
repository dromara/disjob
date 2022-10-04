package cn.ponfee.scheduler.samples;

import cn.ponfee.scheduler.core.base.HttpProperties;
import cn.ponfee.scheduler.springboot.configure.*;
import cn.ponfee.scheduler.supervisor.base.SupervisorProperties;
import cn.ponfee.scheduler.worker.base.WorkerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Scheduler application based spring boot
 *
 * @author Ponfee
 */
@EnableConfigurationProperties({SupervisorProperties.class, WorkerProperties.class, HttpProperties.class})
@EnableSupervisor
@EnableWorker
@EnableRedisServerRegistry
@EnableRedisTaskDispatching
//@EnableHttpTaskDispatching
@SpringBootApplication(
    exclude = {
        DataSourceAutoConfiguration.class
    },
    scanBasePackages = {
        "cn.ponfee.scheduler.supervisor",
        "cn.ponfee.scheduler.worker",
        "cn.ponfee.scheduler.samples.config"
    }
)
public class SchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulerApplication.class, args);
    }

}

package cn.ponfee.scheduler.samples;

import cn.ponfee.scheduler.springboot.configure.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * Scheduler application based spring boot
 *
 * @author Ponfee
 */
@EnableSupervisor
@EnableWorker
@EnableRedisServerRegistry
@EnableRedisTaskDispatcher
//@EnableHttpTaskDispatcher
@SpringBootApplication(
    exclude = {
        DataSourceAutoConfiguration.class
    },
    scanBasePackages = {
        "cn.ponfee.scheduler.supervisor",
        "cn.ponfee.scheduler.worker",
        "cn.ponfee.scheduler.samples"
    }
)
public class SchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulerApplication.class, args);
    }

}

package cn.ponfee.scheduler.samples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * Scheduler application based spring boot
 *
 * @author Ponfee
 */
@SpringBootApplication(
    exclude = {DataSourceAutoConfiguration.class},
    scanBasePackages = {"cn.ponfee.scheduler"}
)
public class SchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulerApplication.class, args);
    }

}

package cn.ponfee.scheduler.supervisor.samples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * Supervisor application based spring boot
 *
 * @author Ponfee
 */
@SpringBootApplication(
    exclude = {DataSourceAutoConfiguration.class},
    scanBasePackages = {"cn.ponfee.scheduler.supervisor"}
)
public class SupervisorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupervisorApplication.class, args);
    }

}

package cn.ponfee.scheduler.supervisor.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * Bootstrap application
 *
 * @author Ponfee
 */
@SpringBootApplication(
    exclude = {
        DataSourceAutoConfiguration.class
    },
    scanBasePackages = {
        "cn.ponfee.scheduler.supervisor"
    }
)
public class SupervisorTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupervisorTestApplication.class, args);
    }

}

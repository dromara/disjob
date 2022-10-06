package cn.ponfee.scheduler.supervisor;

import cn.ponfee.scheduler.core.base.HttpProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Bootstrap application
 *
 * @author Ponfee
 */
@EnableConfigurationProperties(HttpProperties.class)
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

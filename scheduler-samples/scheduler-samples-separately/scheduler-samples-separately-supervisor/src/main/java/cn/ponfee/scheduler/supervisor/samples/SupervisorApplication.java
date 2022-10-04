package cn.ponfee.scheduler.supervisor.samples;

import cn.ponfee.scheduler.core.base.HttpProperties;
import cn.ponfee.scheduler.springboot.configure.*;
import cn.ponfee.scheduler.supervisor.base.SupervisorProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Supervisor application based spring boot
 *
 * @author Ponfee
 */
@EnableConfigurationProperties({SupervisorProperties.class, HttpProperties.class})
@EnableSupervisor
@EnableRedisServerRegistry
@EnableRedisTaskDispatching
//@EnableHttpTaskDispatching
@SpringBootApplication(
    exclude = {
        DataSourceAutoConfiguration.class
    },
    scanBasePackages = {
        "cn.ponfee.scheduler.supervisor"
    }
)
public class SupervisorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupervisorApplication.class, args);
    }

}

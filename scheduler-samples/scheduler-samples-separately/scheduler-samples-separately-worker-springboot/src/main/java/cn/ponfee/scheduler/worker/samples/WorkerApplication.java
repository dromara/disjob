package cn.ponfee.scheduler.worker.samples;

import cn.ponfee.scheduler.core.base.HttpProperties;
import cn.ponfee.scheduler.springboot.configure.*;
import cn.ponfee.scheduler.worker.base.WorkerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Worker application based spring boot
 *
 * @author Ponfee
 */
@EnableConfigurationProperties({WorkerProperties.class, HttpProperties.class})
@EnableWorker
@EnableRedisServerRegistry
@EnableRedisTaskDispatching
//@EnableHttpTaskDispatching
@SpringBootApplication(
    exclude = {
        DataSourceAutoConfiguration.class
    },
    scanBasePackages = {
        "cn.ponfee.scheduler.worker"
    }
)
public class WorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkerApplication.class, args);
    }

}

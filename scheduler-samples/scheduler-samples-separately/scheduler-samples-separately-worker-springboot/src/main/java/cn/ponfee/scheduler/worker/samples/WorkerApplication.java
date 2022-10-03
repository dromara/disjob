package cn.ponfee.scheduler.worker.samples;

import cn.ponfee.scheduler.springboot.configure.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * Worker application based spring boot
 *
 * @author Ponfee
 */
@EnableWorker
@EnableRedisServerRegistry
@EnableRedisTaskDispatcher
//@EnableHttpTaskDispatcher
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

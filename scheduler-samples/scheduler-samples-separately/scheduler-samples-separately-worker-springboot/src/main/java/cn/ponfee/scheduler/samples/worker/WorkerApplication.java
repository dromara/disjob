package cn.ponfee.scheduler.samples.worker;

import cn.ponfee.scheduler.core.base.HttpProperties;
import cn.ponfee.scheduler.dispatch.http.configuration.EnableHttpTaskDispatching;
import cn.ponfee.scheduler.dispatch.redis.configuration.EnableRedisTaskDispatching;
import cn.ponfee.scheduler.registry.consul.configuration.ConsulProperties;
import cn.ponfee.scheduler.registry.consul.configuration.EnableConsulServerRegistry;
import cn.ponfee.scheduler.registry.redis.configuration.EnableRedisServerRegistry;
import cn.ponfee.scheduler.worker.configuration.EnableWorker;
import cn.ponfee.scheduler.worker.configuration.WorkerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Worker application based spring boot
 *
 * @author Ponfee
 */
@EnableConfigurationProperties({
    WorkerProperties.class,
    HttpProperties.class,
    ConsulProperties.class
})
@EnableWorker
@EnableRedisServerRegistry // EnableRedisServerRegistry、EnableConsulServerRegistry
@EnableRedisTaskDispatching // EnableRedisTaskDispatching、EnableHttpTaskDispatching
@SpringBootApplication(
    exclude = {
        DataSourceAutoConfiguration.class
    },
    scanBasePackages = {
        "cn.ponfee.scheduler.samples.common.configuration",
        "cn.ponfee.scheduler.samples.worker.configuration"
    }
)
public class WorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkerApplication.class, args);
    }

}

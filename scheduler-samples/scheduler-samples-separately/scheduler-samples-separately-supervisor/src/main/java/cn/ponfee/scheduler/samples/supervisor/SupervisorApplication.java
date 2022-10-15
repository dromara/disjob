package cn.ponfee.scheduler.samples.supervisor;

import cn.ponfee.scheduler.core.base.HttpProperties;
import cn.ponfee.scheduler.dispatch.http.configuration.EnableHttpTaskDispatching;
import cn.ponfee.scheduler.dispatch.redis.configuration.EnableRedisTaskDispatching;
import cn.ponfee.scheduler.registry.consul.configuration.ConsulProperties;
import cn.ponfee.scheduler.registry.consul.configuration.EnableConsulServerRegistry;
import cn.ponfee.scheduler.registry.redis.configuration.EnableRedisServerRegistry;
import cn.ponfee.scheduler.registry.zookeeper.configuration.EnableZookeeperServerRegistry;
import cn.ponfee.scheduler.registry.zookeeper.configuration.ZookeeperProperties;
import cn.ponfee.scheduler.supervisor.configuration.EnableSupervisor;
import cn.ponfee.scheduler.supervisor.configuration.SupervisorProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Supervisor application based spring boot
 *
 * @author Ponfee
 */
@EnableConfigurationProperties({
    SupervisorProperties.class,
    HttpProperties.class,
    ConsulProperties.class,
    ZookeeperProperties.class
})
@EnableSupervisor
@EnableZookeeperServerRegistry // EnableRedisServerRegistry、EnableConsulServerRegistry、EnableZookeeperServerRegistry
@EnableRedisTaskDispatching // EnableRedisTaskDispatching、EnableHttpTaskDispatching
@SpringBootApplication(
    exclude = {
        DataSourceAutoConfiguration.class
    },
    scanBasePackages = {
        "cn.ponfee.scheduler.samples.common.configuration",
        "cn.ponfee.scheduler.samples.supervisor.configuration",
        "cn.ponfee.scheduler.supervisor",
    }
)
public class SupervisorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupervisorApplication.class, args);
    }

}

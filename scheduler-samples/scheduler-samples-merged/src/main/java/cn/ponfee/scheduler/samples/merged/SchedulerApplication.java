/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.samples.merged;

import cn.ponfee.scheduler.common.base.Snowflake;
import cn.ponfee.scheduler.core.base.HttpProperties;
import cn.ponfee.scheduler.dispatch.http.configuration.EnableHttpTaskDispatching;
import cn.ponfee.scheduler.dispatch.redis.configuration.EnableRedisTaskDispatching;
import cn.ponfee.scheduler.registry.consul.configuration.ConsulRegistryProperties;
import cn.ponfee.scheduler.registry.consul.configuration.EnableConsulServerRegistry;
import cn.ponfee.scheduler.registry.etcd.configuration.EnableEtcdServerRegistry;
import cn.ponfee.scheduler.registry.etcd.configuration.EtcdRegistryProperties;
import cn.ponfee.scheduler.registry.nacos.configuration.EnableNacosServerRegistry;
import cn.ponfee.scheduler.registry.nacos.configuration.NacosRegistryProperties;
import cn.ponfee.scheduler.registry.redis.configuration.EnableRedisServerRegistry;
import cn.ponfee.scheduler.registry.redis.configuration.RedisRegistryProperties;
import cn.ponfee.scheduler.registry.zookeeper.configuration.EnableZookeeperServerRegistry;
import cn.ponfee.scheduler.registry.zookeeper.configuration.ZookeeperRegistryProperties;
import cn.ponfee.scheduler.supervisor.configuration.EnableSupervisor;
import cn.ponfee.scheduler.supervisor.configuration.SupervisorProperties;
import cn.ponfee.scheduler.worker.configuration.EnableWorker;
import cn.ponfee.scheduler.worker.configuration.WorkerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * Scheduler application based spring boot
 *
 * @author Ponfee
 */
@Import(Snowflake.class)
@EnableConfigurationProperties({
    SupervisorProperties.class,
    WorkerProperties.class,
    HttpProperties.class,
    RedisRegistryProperties.class,
    ConsulRegistryProperties.class,
    NacosRegistryProperties.class,
    ZookeeperRegistryProperties.class,
    EtcdRegistryProperties.class,
})
@EnableSupervisor
@EnableWorker
@EnableRedisServerRegistry // EnableRedisServerRegistry、EnableConsulServerRegistry、EnableNacosServerRegistry、EnableZookeeperServerRegistry、EnableEtcdServerRegistry
@EnableRedisTaskDispatching // EnableRedisTaskDispatching、EnableHttpTaskDispatching
@SpringBootApplication(
    exclude = {
        DataSourceAutoConfiguration.class
    },
    scanBasePackages = {
        "cn.ponfee.scheduler.supervisor",
        "cn.ponfee.scheduler.samples.common.configuration",
        "cn.ponfee.scheduler.samples.merged.configuration",
    }
)
public class SchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulerApplication.class, args);
    }

}

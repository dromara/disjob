/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.samples.supervisor;

import cn.ponfee.scheduler.dispatch.http.configuration.EnableHttpTaskDispatching;
import cn.ponfee.scheduler.dispatch.redis.configuration.EnableRedisTaskDispatching;
import cn.ponfee.scheduler.registry.consul.configuration.EnableConsulServerRegistry;
import cn.ponfee.scheduler.registry.etcd.configuration.EnableEtcdServerRegistry;
import cn.ponfee.scheduler.registry.nacos.configuration.EnableNacosServerRegistry;
import cn.ponfee.scheduler.registry.redis.configuration.EnableRedisServerRegistry;
import cn.ponfee.scheduler.registry.zookeeper.configuration.EnableZookeeperServerRegistry;
import cn.ponfee.scheduler.samples.common.AbstractSchedulerSamplesApplication;
import cn.ponfee.scheduler.supervisor.configuration.EnableSupervisor;
import org.springframework.boot.SpringApplication;

/**
 * Supervisor application based spring boot
 *
 * @author Ponfee
 */
@EnableSupervisor
@EnableRedisServerRegistry // EnableRedisServerRegistry, EnableConsulServerRegistry, EnableNacosServerRegistry, EnableZookeeperServerRegistry, EnableEtcdServerRegistry
@EnableRedisTaskDispatching // EnableRedisTaskDispatching, EnableHttpTaskDispatching
public class SupervisorApplication extends AbstractSchedulerSamplesApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupervisorApplication.class, args);
    }

}

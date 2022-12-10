package cn.ponfee.scheduler.registry.nacos;

import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import cn.ponfee.scheduler.registry.nacos.configuration.NacosProperties;

/**
 * Registry supervisor based nacos.
 *
 * @author Ponfee
 */
public class NacosSupervisorRegistry extends NacosServerRegistry<Supervisor, Worker> implements SupervisorRegistry {

    public NacosSupervisorRegistry(String namespace, NacosProperties config) {
        super(namespace, config);
    }

}

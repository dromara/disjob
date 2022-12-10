package cn.ponfee.scheduler.registry.nacos;

import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.WorkerRegistry;
import cn.ponfee.scheduler.registry.nacos.configuration.NacosProperties;

/**
 * Registry worker based nacos.
 *
 * @author Ponfee
 */
public class NacosWorkerRegistry extends NacosServerRegistry<Worker, Supervisor> implements WorkerRegistry {

    public NacosWorkerRegistry(String namespace, NacosProperties config) {
        super(namespace, config);
    }

}

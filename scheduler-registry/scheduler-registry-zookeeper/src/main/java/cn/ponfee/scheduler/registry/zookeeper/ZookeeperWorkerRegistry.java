package cn.ponfee.scheduler.registry.zookeeper;

import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.WorkerRegistry;
import cn.ponfee.scheduler.registry.zookeeper.configuration.ZookeeperProperties;

/**
 * Registry worker based zookeeper.
 *
 * @author Ponfee
 */
public class ZookeeperWorkerRegistry extends ZookeeperServerRegistry<Worker, Supervisor> implements WorkerRegistry {

    public ZookeeperWorkerRegistry(ZookeeperProperties props) {
        super(props);
    }

}

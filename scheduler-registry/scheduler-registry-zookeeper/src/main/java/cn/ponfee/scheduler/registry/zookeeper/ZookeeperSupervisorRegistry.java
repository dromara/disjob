package cn.ponfee.scheduler.registry.zookeeper;

import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import cn.ponfee.scheduler.registry.zookeeper.configuration.ZookeeperProperties;

/**
 * Registry supervisor based zookeeper.
 *
 * @author Ponfee
 */
public class ZookeeperSupervisorRegistry extends ZookeeperServerRegistry<Supervisor, Worker> implements SupervisorRegistry {

    public ZookeeperSupervisorRegistry(String namespace, ZookeeperProperties props) {
        super(namespace, props);
    }

}

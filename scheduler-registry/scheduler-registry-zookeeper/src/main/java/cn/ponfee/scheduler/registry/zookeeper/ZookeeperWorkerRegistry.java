package cn.ponfee.scheduler.registry.zookeeper;

import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.WorkerRegistry;
import cn.ponfee.scheduler.registry.zookeeper.configuration.ZookeeperProperties;

import java.util.Collections;
import java.util.List;

/**
 * Registry worker based zookeeper.
 *
 * @author Ponfee
 */
public class ZookeeperWorkerRegistry extends ZookeeperServerRegistry<Worker, Supervisor> implements WorkerRegistry {

    private volatile List<Supervisor> supervisors = Collections.emptyList();

    public ZookeeperWorkerRegistry(ZookeeperProperties props) {
        super(props);
    }

    @Override
    public List<Supervisor> getServers(String group) {
        return supervisors;
    }

    @Override
    protected void doRefreshDiscoveryServers(List<Supervisor> servers) {
        this.supervisors = servers;
    }

    @Override
    public void close() {
        super.close();
        this.supervisors = null;
    }

}

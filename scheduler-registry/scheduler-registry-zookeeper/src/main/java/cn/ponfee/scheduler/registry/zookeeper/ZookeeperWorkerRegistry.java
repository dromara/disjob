package cn.ponfee.scheduler.registry.zookeeper;

import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.WorkerRegistry;
import cn.ponfee.scheduler.registry.zookeeper.configuration.ZookeeperProperties;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;

/**
 * Registry worker based zookeeper.
 *
 * @author Ponfee
 */
public class ZookeeperWorkerRegistry extends ZookeeperServerRegistry<Worker, Supervisor> implements WorkerRegistry {

    private volatile List<Supervisor> supervisors;

    public ZookeeperWorkerRegistry(ZookeeperProperties props) {
        super(props);
    }

    @Override
    public List<Supervisor> getServers(String group) {
        Assert.isNull(group, "Supervisor non grouped, group must be null.");
        return supervisors;
    }

    @Override
    protected void refreshDiscoveryServers(List<Supervisor> servers) {
        this.supervisors = CollectionUtils.isEmpty(servers)
                         ? Collections.emptyList()
                         : Collections.unmodifiableList(servers);
    }

    @Override
    public void close() {
        super.close();
        this.supervisors = null;
    }

}

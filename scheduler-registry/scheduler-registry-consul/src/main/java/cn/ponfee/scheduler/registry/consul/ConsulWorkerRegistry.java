package cn.ponfee.scheduler.registry.consul;

import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.WorkerRegistry;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;

/**
 * Registry worker based consul.
 *
 * @author Ponfee
 */
public class ConsulWorkerRegistry extends ConsulServerRegistry<Worker, Supervisor> implements WorkerRegistry {

    private volatile List<Supervisor> supervisors;

    public ConsulWorkerRegistry(String host, int port, String token) {
        super(host, port, token);
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

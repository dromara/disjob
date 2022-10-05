package cn.ponfee.scheduler.registry.consul;

import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.WorkerRegistry;

import java.util.Collections;
import java.util.List;

/**
 * Registry worker based consul.
 *
 * @author Ponfee
 */
public class ConsulWorkerRegistry extends ConsulServerRegistry<Worker, Supervisor> implements WorkerRegistry {

    private volatile List<Supervisor> supervisors = Collections.emptyList();

    public ConsulWorkerRegistry(String host, int port, String token) {
        super(host, port, token);
    }

    @Override
    public List<Supervisor> getServers(String group) {
        return supervisors;
    }

    @Override
    protected void doRefreshDiscoveryServers(List<Supervisor> servers) {
        this.supervisors = servers;
    }

}

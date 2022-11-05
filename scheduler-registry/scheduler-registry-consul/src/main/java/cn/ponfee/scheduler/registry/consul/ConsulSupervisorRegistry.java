package cn.ponfee.scheduler.registry.consul;

import cn.ponfee.scheduler.common.base.DoubleListViewer;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Registry supervisor based consul.
 *
 * @author Ponfee
 */
public class ConsulSupervisorRegistry extends ConsulServerRegistry<Supervisor, Worker> implements SupervisorRegistry {

    private volatile Map<String, List<Worker>> groupedWorkers;
    private volatile List<Worker> allWorkers;

    public ConsulSupervisorRegistry(String host, int port, String token) {
        super(host, port, token);
    }

    @Override
    public List<Worker> getServers(String group) {
        return group == null ? allWorkers : groupedWorkers.get(group);
    }

    @Override
    protected void refreshDiscoveryServers(List<Worker> discoveredWorkers) {
        if (CollectionUtils.isEmpty(discoveredWorkers)) {
            this.groupedWorkers = Collections.emptyMap();
            this.allWorkers = Collections.emptyList();
        } else {
            Map<String, List<Worker>> map = SupervisorRegistry.groupByWorkers(discoveredWorkers);
            DoubleListViewer<Worker> list = new DoubleListViewer<>(map.values());
            this.groupedWorkers = map;
            this.allWorkers = list;
        }
    }

    @Override
    public void close() {
        super.close();
        this.groupedWorkers = null;
        this.allWorkers = null;
    }

}

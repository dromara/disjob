package cn.ponfee.scheduler.registry.consul;

import cn.ponfee.scheduler.common.base.DoubleListViewer;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Registry supervisor based consul.
 *
 * @author Ponfee
 */
public class ConsulSupervisorRegistry extends ConsulServerRegistry<Supervisor, Worker> implements SupervisorRegistry {

    private volatile Map<String, List<Worker>> groupedWorkers = Collections.emptyMap();
    private volatile List<Worker> allWorkers = new DoubleListViewer<>(Collections.emptyList());

    public ConsulSupervisorRegistry(String host, int port, String token) {
        super(host, port, token);
    }

    @Override
    public List<Worker> getServers(String group) {
        return group == null ? allWorkers : groupedWorkers.get(group);
    }

    @Override
    protected void doRefreshDiscoveryServers(List<Worker> discoveredWorkers) {
        if (CollectionUtils.isEmpty(discoveredWorkers)) {
            this.groupedWorkers = Collections.emptyMap();
            this.allWorkers = Collections.emptyList();
        } else {
            Map<String, List<Worker>> groupedWorkers0 = discoveredWorkers.stream()
                .collect(Collectors.groupingBy(Worker::getGroup))
                .entrySet()
                .stream()
                .peek(e -> e.getValue().sort(Comparator.comparing(Worker::getInstanceId)))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> Collections.unmodifiableList(e.getValue())));

            DoubleListViewer<Worker> allWorkers0 = new DoubleListViewer<>(groupedWorkers0.values());

            this.groupedWorkers = groupedWorkers0;
            this.allWorkers = allWorkers0;
        }
    }

}

package cn.ponfee.scheduler.registry;

import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Supervisor registry and discovery worker.
 *
 * @author Ponfee
 */
public interface SupervisorRegistry extends Registry<Supervisor>, Discovery<Worker>, AutoCloseable {

    /**
     * Close registry.
     */
    @Override
    void close();

    /**
     * Group by discovered workers.
     *
     * @param discoveredWorkers the discoveredWorkers
     * @return map of grouped by discovered workers
     */
    static Map<String, List<Worker>> groupByWorkers(List<Worker> discoveredWorkers) {
        return discoveredWorkers.stream()
            .collect(Collectors.groupingBy(Worker::getGroup))
            .entrySet()
            .stream()
            .peek(e -> e.getValue().sort(Comparator.comparing(Worker::getInstanceId))) // For help use route worker
            .collect(Collectors.toMap(Map.Entry::getKey, e -> Collections.unmodifiableList(e.getValue())));
    }

}

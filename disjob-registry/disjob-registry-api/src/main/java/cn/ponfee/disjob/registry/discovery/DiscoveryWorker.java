/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.registry.discovery;

import cn.ponfee.disjob.common.collect.ImmutableHashList;
import cn.ponfee.disjob.core.base.Worker;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Discovery worker.
 *
 * @author Ponfee
 */
public final class DiscoveryWorker implements DiscoveryServer<Worker> {

    /**
     * Map<group, ImmutableHashList<serialize, Worker>>
     */
    private volatile Map<String, ImmutableHashList<String, Worker>> groupedWorkers = Collections.emptyMap();

    @Override
    public void refreshServers(List<Worker> discoveredWorkers) {
        if (CollectionUtils.isEmpty(discoveredWorkers)) {
            this.groupedWorkers = Collections.emptyMap();
        } else {
            this.groupedWorkers = discoveredWorkers.stream()
                .collect(Collectors.groupingBy(Worker::getGroup))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> ImmutableHashList.of(e.getValue(), Worker::serialize)));
        }
    }

    @Override
    public List<Worker> getServers(String group) {
        Assert.hasText(group, "Get discovery worker group cannot null.");
        ImmutableHashList<String, Worker> workers = groupedWorkers.get(group);
        return workers == null ? Collections.emptyList() : workers.values();
    }

    @Override
    public boolean hasServers() {
        return !groupedWorkers.isEmpty();
    }

    @Override
    public boolean isAlive(Worker worker) {
        ImmutableHashList<String, Worker> workers = groupedWorkers.get(worker.getGroup());
        return workers != null && workers.contains(worker);
    }

}

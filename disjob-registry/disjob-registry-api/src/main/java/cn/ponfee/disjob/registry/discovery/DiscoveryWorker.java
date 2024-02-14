/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    public synchronized void refreshServers(List<Worker> discoveredWorkers) {
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

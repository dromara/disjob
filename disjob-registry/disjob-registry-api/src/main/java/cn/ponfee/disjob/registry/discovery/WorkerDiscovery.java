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

import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.base.WorkerRpcService;
import cn.ponfee.disjob.core.dto.worker.SupervisorEventParam;
import cn.ponfee.disjob.core.enums.RegistryEventType;
import cn.ponfee.disjob.registry.rpc.DestinationServerRestProxy;
import cn.ponfee.disjob.registry.rpc.DestinationServerRestProxy.DestinationServerClient;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Worker discovery.
 *
 * @author Ponfee
 */
public final class WorkerDiscovery extends ServerDiscovery<Worker, Supervisor> {

    private final DestinationServerClient<WorkerRpcService, Worker> workerRpcClient;

    /**
     * Map<group, List<Worker>>
     */
    private volatile ImmutableMap<String, ImmutableList<Worker>> groupedWorkers = ImmutableMap.of();

    WorkerDiscovery(RestTemplate restTemplate) {
        this.workerRpcClient = DestinationServerRestProxy.create(
            WorkerRpcService.class,
            null,
            null,
            worker -> Supervisor.local().getWorkerContextPath(worker.getGroup()),
            restTemplate,
            RetryProperties.none()
        );
    }

    // ----------------------------------------------------------------write methods

    @Override
    public synchronized void refresh(List<Worker> discoveredWorkers) {
        if (CollectionUtils.isEmpty(discoveredWorkers)) {
            this.groupedWorkers = ImmutableMap.of();
        } else {
            this.groupedWorkers = discoveredWorkers.stream()
                .collect(Collectors.groupingBy(Worker::getGroup))
                .entrySet()
                .stream()
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, e -> toSortedImmutableList(e.getValue())));
        }
    }

    @Override
    public synchronized void update(RegistryEventType eventType, Worker worker) {
        // if register and not exists worker, or deregister and exists worker
        if (eventType.isRegister() != isAliveServer(worker)) {
            final ImmutableMap<String, ImmutableList<Worker>> map = groupedWorkers;
            String group = worker.getGroup();
            ImmutableMap.Builder<String, ImmutableList<Worker>> builder = ImmutableMap.builder();
            map.forEach((k, v) -> builder.put(k, group.equals(k) ? merge(v, eventType, worker) : v));
            if (!map.containsKey(group)) {
                builder.put(group, ImmutableList.of(worker));
            }
            this.groupedWorkers = builder.build();
        }
    }

    @Override
    void notify(Worker worker, RegistryEventType eventType, Supervisor supervisor) {
        try {
            SupervisorEventParam param = SupervisorEventParam.of(worker.getGroup(), eventType, supervisor);
            workerRpcClient.invoke(worker, client -> client.subscribeSupervisorEvent(param));
        } catch (Throwable t) {
            log.error("Notify server error: {}, {}", worker, t.getMessage());
        }
    }

    // ----------------------------------------------------------------read methods

    @Override
    public List<Worker> getAliveServers(String group) {
        Assert.hasText(group, "Get alive workers group cannot be null.");
        List<Worker> workers = groupedWorkers.get(group);
        return workers == null ? Collections.emptyList() : workers;
    }

    @Override
    List<Worker> getAliveServers() {
        Collection<ImmutableList<Worker>> values = groupedWorkers.values();
        List<Worker> list = new ArrayList<>(values.stream().mapToInt(Collection::size).sum());
        values.forEach(list::addAll);
        return list;
    }

    @Override
    public boolean hasAliveServer() {
        return !groupedWorkers.isEmpty();
    }

    @Override
    public boolean isAliveServer(Worker worker) {
        final List<Worker> workers = groupedWorkers.get(worker.getGroup());
        return workers != null && Collections.binarySearch(workers, worker) > -1;
    }

}

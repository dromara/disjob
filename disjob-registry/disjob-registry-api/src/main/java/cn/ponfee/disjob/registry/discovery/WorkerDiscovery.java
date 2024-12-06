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
import cn.ponfee.disjob.core.dto.worker.SubscribeSupervisorChangedParam;
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

    @Override
    public synchronized void refreshServers(List<Worker> discoveredWorkers) {
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
    public synchronized void updateServers(RegistryEventType eventType, Worker worker) {
        if (eventType.isRegister() && isAlive(worker)) {
            return;
        }
        if (eventType.isDeregister() && !isAlive(worker)) {
            return;
        }

        final ImmutableMap<String, ImmutableList<Worker>> map = groupedWorkers;
        String group = worker.getGroup();
        ImmutableMap.Builder<String, ImmutableList<Worker>> builder = ImmutableMap.builder();
        map.forEach((k, v) -> builder.put(k, group.equals(k) ? mergeServers(v, eventType, worker) : v));
        if (!map.containsKey(group)) {
            builder.put(group, ImmutableList.of(worker));
        }
        this.groupedWorkers = builder.build();
    }

    @Override
    public List<Worker> getServers(String group) {
        Assert.hasText(group, "Get discovery worker group cannot null.");
        List<Worker> workers = groupedWorkers.get(group);
        return workers == null ? Collections.emptyList() : workers;
    }

    @Override
    public boolean hasServers() {
        return !groupedWorkers.isEmpty();
    }

    @Override
    public boolean isAlive(Worker worker) {
        List<Worker> workers = groupedWorkers.get(worker.getGroup());
        return workers != null && Collections.binarySearch(workers, worker) > -1;
    }

    // ----------------------------------------------------------------default package methods

    @Override
    List<Worker> getServers() {
        Collection<ImmutableList<Worker>> values = groupedWorkers.values();
        List<Worker> list = new ArrayList<>(values.stream().mapToInt(Collection::size).sum());
        values.forEach(list::addAll);
        return list;
    }

    @Override
    void notifyServer(Worker worker, RegistryEventType eventType, Supervisor supervisor) {
        /*
        if (worker.matches(Worker.local())) {
            return;
        }
        */
        try {
            String authToken = Supervisor.local().createSupervisorAuthenticationToken(worker.getGroup());
            SubscribeSupervisorChangedParam param = SubscribeSupervisorChangedParam.of(authToken, eventType, supervisor);
            workerRpcClient.invoke(worker, client -> client.subscribeSupervisorChanged(param));
        } catch (Throwable t) {
            log.error("Notify server error: {}, {}", worker, t.getMessage());
        }
    }

}

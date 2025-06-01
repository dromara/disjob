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

import cn.ponfee.disjob.common.concurrent.MultithreadExecutors;
import cn.ponfee.disjob.common.concurrent.NamedThreadFactory;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.core.enums.RegistryEventType;
import cn.ponfee.disjob.registry.ServerRole;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.stream.Stream;

/**
 * Server discovery.
 *
 * @author Ponfee
 */
public abstract class ServerDiscovery<D extends Server, R extends Server> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    // ----------------------------------------------------------------write methods

    public abstract void refresh(List<D> servers);

    public abstract void update(RegistryEventType eventType, D server);

    public final void notify(RegistryEventType eventType, R rServer) {
        List<D> servers = getAliveServers();
        if (CollectionUtils.isEmpty(servers)) {
            return;
        }
        if (servers.size() == 1) {
            notify(servers.get(0), eventType, rServer);
            return;
        }
        ExecutorService threadPool = ThreadPoolExecutors.builder()
            .corePoolSize(1)
            .maximumPoolSize(Math.min(servers.size() - 1, 100))
            .workQueue(new SynchronousQueue<>())
            .keepAliveTimeSeconds(60)
            .rejectedHandler(ThreadPoolExecutors.CALLER_RUNS)
            .threadFactory(NamedThreadFactory.builder().prefix("notify_server").uncaughtExceptionHandler(log).build())
            .build();
        MultithreadExecutors.run(servers, dServer -> notify(dServer, eventType, rServer), threadPool);
        threadPool.shutdown();
    }

    abstract void notify(D dServer, RegistryEventType eventType, R rServer);

    // ----------------------------------------------------------------read methods

    public abstract List<D> getAliveServers(String group);

    abstract List<D> getAliveServers();

    public abstract boolean hasAliveServer();

    public abstract boolean isAliveServer(D server);

    // ----------------------------------------------------------------static methods

    static <D extends Server> ImmutableList<D> merge(ImmutableList<D> servers, RegistryEventType eventType, D server) {
        Stream<D> stream;
        if (eventType.isRegister()) {
            stream = Stream.concat(servers.stream(), Stream.of(server));
        } else {
            stream = servers.stream().filter(e -> !e.equals(server));
        }
        return toSortedImmutableList(stream);
    }

    /**
     * Returns sorted ImmutableList
     *
     * @param servers the server list
     * @return sorted ImmutableList
     */
    static <D extends Server> ImmutableList<D> toSortedImmutableList(List<D> servers) {
        return CollectionUtils.isEmpty(servers) ? ImmutableList.of() : toSortedImmutableList(servers.stream());
    }

    public static <D extends Server> ImmutableList<D> toSortedImmutableList(Stream<D> stream) {
        return stream.sorted().collect(ImmutableList.toImmutableList());
    }

    @SuppressWarnings("unchecked")
    public static <D extends Server, R extends Server> ServerDiscovery<D, R> of(ServerRole discoveryRole,
                                                                                RestTemplate restTemplate) {
        switch (discoveryRole) {
            case WORKER:
                return (ServerDiscovery<D, R>) new WorkerDiscovery(restTemplate);
            case SUPERVISOR:
                return (ServerDiscovery<D, R>) new SupervisorDiscovery(restTemplate);
            default:
                throw new UnsupportedOperationException("Unsupported server discovery role '" + discoveryRole + "'");
        }
    }

}

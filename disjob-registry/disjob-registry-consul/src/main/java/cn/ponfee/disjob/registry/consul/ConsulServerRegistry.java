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

package cn.ponfee.disjob.registry.consul;

import cn.ponfee.disjob.common.concurrent.LoggedUncaughtExceptionHandler;
import cn.ponfee.disjob.common.concurrent.LoopThread;
import cn.ponfee.disjob.common.concurrent.Threads;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.registry.ServerRegistry;
import cn.ponfee.disjob.registry.consul.configuration.ConsulRegistryProperties;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.OperationException;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Registry server based <a href="https://developer.hashicorp.com/consul/api-docs">consul</a>.
 *
 * @author Ponfee
 */
public abstract class ConsulServerRegistry<R extends Server, D extends Server> extends ServerRegistry<R, D> {

    private static final int WAIT_TIME_SECONDS = 60;
    private static final int CHECK_PASS_INTERVAL_SECONDS = 2;
    private static final String CHECK_TTL_SECONDS = (CHECK_PASS_INTERVAL_SECONDS * 8) + "s";
    private static final String DEREGISTER_TIME_SECONDS = "20s";

    /**
     * Consul client
     */
    private final ConsulClient client;

    /**
     * Consul ACL token
     */
    private final String token;

    /**
     * Consul ttl check thread
     */
    private final LoopThread consulTtlCheckThread;

    private final ConsulSubscriberThread consulSubscriberThread;

    protected ConsulServerRegistry(ConsulRegistryProperties config) {
        super(config, ':');

        this.client = new ConsulClient(config.getHost(), config.getPort());
        this.token = StringUtils.isBlank(config.getToken()) ? null : config.getToken().trim();

        int periodMs = Math.max(CHECK_PASS_INTERVAL_SECONDS, 1) * 1000;
        this.consulTtlCheckThread = LoopThread.createStarted("consul_ttl_check", periodMs, periodMs, this::checkPass);

        this.consulSubscriberThread = new ConsulSubscriberThread(-1);
        consulSubscriberThread.start();
    }

    @Override
    public final boolean isConnected() {
        return client.getAgentSelf() != null;
    }

    // ------------------------------------------------------------------Registry

    @Override
    public final void register(R server) {
        if (state.isStopped()) {
            return;
        }

        NewService newService = createService(server);
        if (token == null) {
            client.agentServiceRegister(newService);
        } else {
            client.agentServiceRegister(newService, token);
        }
        registered.add(server);
        log.info("Consul server registered: {}, {}", registryRole, server);
    }

    @Override
    public final void deregister(R server) {
        try {
            registered.remove(server);
            String serverId = buildServiceId(server);
            if (token == null) {
                client.agentServiceDeregister(serverId);
            } else {
                client.agentServiceDeregister(serverId, token);
            }
            log.info("Consul Server deregister: {}, {}", registryRole, server);
        } catch (Throwable t) {
            log.error("Consul server deregister error.", t);
        }
    }

    @Override
    public List<R> getRegisteredServers() {
        HealthServicesRequest request = HealthServicesRequest.newBuilder()
            .setPassing(true)
            .setToken(token)
            .build();
        List<HealthService> list = client.getHealthServices(registryRootPath, request).getValue();
        return deserializeRegistryServers(list, e -> e.getService().getId().substring(registryRootPath.length() + 1));
    }

    // ------------------------------------------------------------------Close

    @PreDestroy
    @Override
    public void close() {
        if (!state.stop()) {
            return;
        }

        consulTtlCheckThread.terminate();
        registered.forEach(this::deregister);
        registered.clear();
        ThrowingRunnable.doCaught(() -> Threads.stopThread(consulSubscriberThread, 1000));
        super.close();
    }

    // ------------------------------------------------------------------private method & class

    /**
     * Builds a unique service id of consul cluster.
     *
     * @param server the registry server
     * @return a string value representing unique consul service id
     */
    private String buildServiceId(R server) {
        return registryRootPath + separator + server.serialize();
    }

    private NewService createService(R server) {
        NewService service = new NewService();
        service.setName(registryRootPath);
        service.setId(buildServiceId(server));
        service.setAddress(server.getHost());
        service.setPort(server.getPort());
        service.setCheck(createCheck());
        service.setTags(null);
        service.setMeta(null);
        return service;
    }

    private static NewService.Check createCheck() {
        NewService.Check check = new NewService.Check();
        check.setTtl(CHECK_TTL_SECONDS);
        check.setDeregisterCriticalServiceAfter(DEREGISTER_TIME_SECONDS);
        return check;
    }

    private void checkPass() {
        if (state.isStopped()) {
            return;
        }
        for (R server : registered) {
            String checkId = buildServiceId(server);
            try {
                // Prepend "service:" for service level checks.
                if (token == null) {
                    client.agentCheckPass("service:" + checkId);
                } else {
                    client.agentCheckPass("service:" + checkId, null, token);
                }
                log.debug("check pass for server: {} with check id {}", server, checkId);
            } catch (Throwable t) {
                if ((t instanceof OperationException) && ((OperationException) t).getStatusCode() == 404) {
                    ThrowingRunnable.doCaught(() -> register(server), () -> "Not found server register failed: " + server);
                    log.warn("Check pass server not found: " + server + ", check id: " + checkId, t);
                } else {
                    log.warn("Check pass server failed: " + server + ", check id: " + checkId, t);
                }
            }
        }
    }

    private class ConsulSubscriberThread extends Thread {
        private long lastConsulIndex;

        private ConsulSubscriberThread(long initConsulIndex) {
            this.lastConsulIndex = initConsulIndex;
            super.setDaemon(true);
            super.setPriority(Thread.MAX_PRIORITY);
            super.setName("consul_subscriber_thread");
            super.setUncaughtExceptionHandler(new LoggedUncaughtExceptionHandler(log));
        }

        @Override
        public void run() {
            while (state.isRunning()) {
                try {
                    Response<List<HealthService>> response = getDiscoveryServers(lastConsulIndex, WAIT_TIME_SECONDS);
                    Long currentIndex = response.getConsulIndex();
                    if (currentIndex != null && currentIndex > lastConsulIndex) {
                        lastConsulIndex = currentIndex;
                        doRefreshDiscoveryServers(response.getValue());
                    }
                } catch (Throwable t) {
                    log.error("Get consul health services occur error.", t);
                    Threads.interruptIfNecessary(t);
                }
            }
        }

        private synchronized void doRefreshDiscoveryServers(List<HealthService> healthServices) {
            List<D> servers;
            if (CollectionUtils.isEmpty(healthServices)) {
                log.warn("Not discovered available {} from consul.", discoveryRole);
                servers = Collections.emptyList();
            } else {
                servers = healthServices.stream()
                    .map(HealthService::getService)
                    .filter(Objects::nonNull)
                    .map(s -> s.getId().substring(discoveryRootPath.length() + 1))
                    .<D>map(discoveryRole::deserialize)
                    .collect(Collectors.toList());
            }
            refreshDiscoveredServers(servers);
        }

        private Response<List<HealthService>> getDiscoveryServers(long index, long waitTime) {
            HealthServicesRequest request = HealthServicesRequest.newBuilder()
                .setQueryParams(new QueryParams(waitTime, index))
                .setPassing(true)
                .setToken(token)
                .build();
            // Health api: /v1/health/service/{serviceName}
            // doc page: https://www.consul.io/api-docs/health
            // Blocking Queries doc page: https://www.consul.io/api-docs/features/blocking
            return client.getHealthServices(discoveryRootPath, request);
        }
    }

}

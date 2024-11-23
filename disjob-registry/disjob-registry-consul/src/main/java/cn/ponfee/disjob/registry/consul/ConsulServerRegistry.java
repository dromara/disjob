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
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Registry server based <a href="https://developer.hashicorp.com/consul/api-docs">consul</a>.
 *
 * @author Ponfee
 */
public abstract class ConsulServerRegistry<R extends Server, D extends Server> extends ServerRegistry<R, D> {

    /**
     * Consul client
     */
    private final ConsulClient client;

    /**
     * Consul ACL token
     */
    private final String token;

    /**
     * Check time to live
     */
    private final String checkTtl;

    /**
     * Check deregister critical timeout
     */
    private final String checkDeregisterCriticalTimeout;

    /**
     * Consul ttl check thread
     */
    private final LoopThread registryTtlScheduler;

    private final DiscoveryWatcher discoveryWatcher;

    protected ConsulServerRegistry(ConsulRegistryProperties config, RestTemplate restTemplate) {
        super(config, restTemplate, ':');

        this.client = new ConsulClient(config.getHost(), config.getPort());
        this.token = StringUtils.isBlank(config.getToken()) ? null : config.getToken().trim();
        this.checkTtl = config.getCheckTtl();
        this.checkDeregisterCriticalTimeout = config.getCheckDeregisterCriticalTimeout();

        int periodMs = Math.max(config.getCheckPassPeriodSeconds(), 1) * 1000;
        this.registryTtlScheduler = LoopThread.createStarted("consul_registry_ttl_scheduler", periodMs, periodMs, this::checkPass);

        this.discoveryWatcher = new DiscoveryWatcher();
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
        Response<List<HealthService>> response = getServers(registryRootPath, null);
        return deserializeServers(extract(response, registryRootPath), registryRole);
    }

    // ------------------------------------------------------------------Close

    @PreDestroy
    @Override
    public void close() {
        if (!state.stop()) {
            return;
        }

        registryTtlScheduler.terminate();
        registered.forEach(this::deregister);
        ThrowingRunnable.doCaught(() -> Threads.stopThread(discoveryWatcher, 1000));
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

    private NewService.Check createCheck() {
        NewService.Check check = new NewService.Check();
        check.setTtl(checkTtl);
        check.setDeregisterCriticalServiceAfter(checkDeregisterCriticalTimeout);
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
            } catch (OperationException e) {
                if (e.getStatusCode() == 404) {
                    ThrowingRunnable.doCaught(() -> register(server), () -> "Not found server register failed: " + server);
                }
                log.warn("Check pass server operation exception: " + server + ", check id: " + checkId, e);
            } catch (Throwable t) {
                log.error("Check pass server error: " + server + ", check id: " + checkId, t);
            }
        }
    }

    private class DiscoveryWatcher extends Thread {
        private long lastConsulIndex = -1;

        private DiscoveryWatcher() {
            super.setDaemon(true);
            super.setPriority(Thread.MAX_PRIORITY);
            super.setName("consul_discovery_watcher_thread");
            super.setUncaughtExceptionHandler(new LoggedUncaughtExceptionHandler(log));
            super.start();
        }

        @Override
        public void run() {
            while (state.isRunning()) {
                try {
                    long waitTimeSeconds = (lastConsulIndex == -1) ? 60 : Integer.MAX_VALUE;
                    QueryParams queryParams = new QueryParams(waitTimeSeconds, lastConsulIndex);
                    Response<List<HealthService>> response = getServers(discoveryRootPath, queryParams);
                    // 当有服务register时此处会执行两次，当有服务deregister时此处只会执行一次
                    Long currentIndex = response.getConsulIndex();
                    if (currentIndex != null && currentIndex > lastConsulIndex) {
                        lastConsulIndex = currentIndex;
                        refreshDiscoveryServers(extract(response, discoveryRootPath));
                    }
                } catch (Throwable t) {
                    log.error("Get consul health services occur error.", t);
                    Threads.interruptIfNecessary(t);
                }
            }
        }
    }

    private Response<List<HealthService>> getServers(String rootPath, QueryParams queryParams) {
        HealthServicesRequest request = HealthServicesRequest.newBuilder()
            .setQueryParams(queryParams)
            .setPassing(true)
            .setToken(token)
            .build();
        // Health api: /v1/health/service/{serviceName}
        // doc page: https://www.consul.io/api-docs/health
        // Blocking Queries doc page: https://www.consul.io/api-docs/features/blocking
        return client.getHealthServices(rootPath, request);
    }

    private static List<String> extract(Response<List<HealthService>> response, String rootPath) {
        List<HealthService> healthServices = response.getValue();
        if (healthServices == null) {
            return null;
        }
        int beginIndex = rootPath.length() + 1;
        return healthServices.stream()
            .filter(Objects::nonNull)
            .map(HealthService::getService)
            .filter(Objects::nonNull)
            .map(HealthService.Service::getId)
            .filter(e -> e != null && e.length() > beginIndex)
            .map(e -> e.substring(beginIndex))
            .collect(Collectors.toList());
    }

}

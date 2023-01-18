/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.registry.consul;

import cn.ponfee.scheduler.common.base.exception.Throwables;
import cn.ponfee.scheduler.common.concurrent.NamedThreadFactory;
import cn.ponfee.scheduler.common.concurrent.Threads;
import cn.ponfee.scheduler.common.util.ObjectUtils;
import cn.ponfee.scheduler.core.base.Server;
import cn.ponfee.scheduler.registry.ServerRegistry;
import cn.ponfee.scheduler.registry.consul.configuration.ConsulRegistryProperties;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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

    // --------------------------------------------------------------registry

    /**
     * Consul client
     */
    private final ConsulClient client;

    /**
     * Consul ACL token
     */
    private final String token;

    private final ScheduledExecutorService consulTtlCheckExecutor;

    private final ConsulSubscriberThread   consulSubscriberThread;

    protected ConsulServerRegistry(String namespace, ConsulRegistryProperties config) {
        super(namespace, ':');

        this.client = new ConsulClient(config.getHost(), config.getPort());
        this.token = config.getToken();

        int period = Math.max(CHECK_PASS_INTERVAL_SECONDS, 1);
        this.consulTtlCheckExecutor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("consul_server_registry", true));
        consulTtlCheckExecutor.scheduleWithFixedDelay(this::checkPass, period, period, TimeUnit.SECONDS);

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
        if (closed.get()) {
            return;
        }

        NewService newService = createService(server);
        if (token == null) {
            client.agentServiceRegister(newService);
        } else {
            client.agentServiceRegister(newService, token);
        }
        registered.add(server);
        log.info("Consul server registered: {} | {}", registryRole.name(), server);
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
            log.info("Consul Server deregister: {} | {}", registryRole.name(), server);
        } catch (Exception e) {
            log.error("Consul server deregister error.", e);
        }
    }

    // ------------------------------------------------------------------Close

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            log.warn("Repeat call close method\n{}", ObjectUtils.getStackTrace());
            return;
        }

        Throwables.caught(consulTtlCheckExecutor::shutdownNow);
        registered.forEach(this::deregister);
        registered.clear();
        Throwables.caught(() -> Threads.stopThread(consulSubscriberThread, 0, 0, 100));
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
        if (closed.get()) {
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
                log.warn("fail to check pass for server: " + server + ", check id is: " + checkId, t);
            }
        }
    }

    private synchronized void doRefreshDiscoveryServers(List<HealthService> healthServices) {
        List<D> servers;
        if (CollectionUtils.isEmpty(healthServices)) {
            log.error("Not discovered available {} from consul.", discoveryRole.name());
            servers = Collections.emptyList();
        } else {
            servers = healthServices.stream()
                .map(HealthService::getService)
                .filter(Objects::nonNull)
                .map(s -> s.getId().substring(discoveryRootPath.length() + 1))
                .map(s -> (D) discoveryRole.deserialize(s))
                .collect(Collectors.toList());
        }
        refreshDiscoveredServers(servers);
    }

    private class ConsulSubscriberThread extends Thread {
        private long lastConsulIndex;

        private ConsulSubscriberThread(long initConsulIndex) {
            this.lastConsulIndex = initConsulIndex;
            super.setDaemon(true);
            super.setName("consul_subscriber_thread");
        }

        @Override
        public void run() {
            while (!closed.get()) {
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

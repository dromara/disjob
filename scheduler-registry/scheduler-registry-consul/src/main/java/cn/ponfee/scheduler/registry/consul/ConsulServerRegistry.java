package cn.ponfee.scheduler.registry.consul;

import cn.ponfee.scheduler.common.base.Constants;
import cn.ponfee.scheduler.common.base.exception.Throwables;
import cn.ponfee.scheduler.common.concurrent.MultithreadExecutors;
import cn.ponfee.scheduler.common.concurrent.NamedThreadFactory;
import cn.ponfee.scheduler.common.util.ObjectUtils;
import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.Server;
import cn.ponfee.scheduler.registry.ServerRegistry;
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
 * Registry server based consul.
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

    // --------------------------------------------------------------discovery

    protected ConsulServerRegistry(String host, int port, String token) {
        this.client = new ConsulClient(host, port);
        this.token = token;

        this.consulTtlCheckExecutor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("Consul-Ttl-Check-Executor", true));
        consulTtlCheckExecutor.scheduleAtFixedRate(this::checkPass, CHECK_PASS_INTERVAL_SECONDS, CHECK_PASS_INTERVAL_SECONDS, TimeUnit.SECONDS);

        this.consulSubscriberThread = new ConsulSubscriberThread(-1);
        consulSubscriberThread.setDaemon(true);
        consulSubscriberThread.setName("Consul-Subscriber-Thread");
        consulSubscriberThread.start();
    }

    // ------------------------------------------------------------------Registry

    @Override
    public final void register(R server) {
        if (closed) {
            return;
        }

        registered.add(server);
        if (token == null) {
            client.agentServiceRegister(buildRegistryServer(server));
        } else {
            client.agentServiceRegister(buildRegistryServer(server), token);
        }
        logger.info("Server registered: {} - {}", registryRole.name(), server.serialize());
    }

    @Override
    public final void deregister(R server) {
        try {
            registered.remove(server);
            if (token == null) {
                client.agentServiceDeregister(buildId(server));
            } else {
                client.agentServiceDeregister(buildId(server), token);
            }
            logger.info("Server deregister: {} - {}", registryRole.name(), server.serialize());
        } catch (Exception e) {
            logger.error("Agent service deregister error.", e);
        }
    }

    /**
     * Refresh discovery servers.
     *
     * @param servers discovered servers
     */
    protected abstract void doRefreshDiscoveryServers(List<D> servers);

    @Override
    public boolean isAlive(D server) {
        return getServers().contains(server);
    }

    @Override
    public void close() {
        closed = true;
        if (!close.compareAndSet(false, true)) {
            logger.warn("Repeat call close method\n{}", ObjectUtils.getStackTrace());
            return;
        }

        try {
            consulTtlCheckExecutor.shutdownNow();
            consulTtlCheckExecutor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Await ttl check pass scheduled executor termination occur error.", e);
        }

        registered.forEach(this::deregister);
        registered.clear();

        Throwables.cached(() -> MultithreadExecutors.stopThread(consulSubscriberThread, 0, 0, 200));
    }

    // ------------------------------------------------------------------private registry methods

    private NewService buildRegistryServer(R server) {
        NewService service = new NewService();
        service.setName(registryRole.registryKey());
        service.setId(buildId(server));
        service.setAddress(server.getHost());
        service.setPort(server.getPort());
        service.setCheck(buildCheck());
        service.setTags(null);
        service.setMeta(null);
        return service;
    }

    private String buildId(R server) {
        return JobConstants.SCHEDULER_KEY_PREFIX + Constants.SLASH + server.serialize();
    }

    private NewService.Check buildCheck() {
        NewService.Check check = new NewService.Check();
        check.setTtl(CHECK_TTL_SECONDS);
        check.setDeregisterCriticalServiceAfter(DEREGISTER_TIME_SECONDS);
        return check;
    }

    private void checkPass() {
        for (R server : registered) {
            String checkId = buildId(server);
            try {
                if (token == null) {
                    client.agentCheckPass("service:" + checkId);
                } else {
                    client.agentCheckPass("service:" + checkId, null, token);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("check pass for server: " + server + " with check id: " + checkId);
                }
            } catch (Throwable t) {
                logger.warn("fail to check pass for server: " + server + ", check id is: " + checkId, t);
            }
        }
    }

    // ------------------------------------------------------------------private discovery methods

    private void refreshDiscoveryServers(List<HealthService> healthServices) {
        List<D> servers;
        if (CollectionUtils.isEmpty(healthServices)) {
            logger.error("Not discovered available server on consul.");
            servers = Collections.emptyList();
        } else {
            servers = healthServices.stream()
                .map(HealthService::getService)
                .filter(Objects::nonNull)
                .map(s -> s.getId().substring(JobConstants.SCHEDULER_KEY_PREFIX.length() + 1))
                .map(s -> (D) discoveryRole.deserialize(s))
                .collect(Collectors.toList());
        }
        doRefreshDiscoveryServers(servers);
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
        return client.getHealthServices(discoveryRole.registryKey(), request);
    }

    private class ConsulSubscriberThread extends Thread {
        private long lastConsulIndex;

        private ConsulSubscriberThread(long initConsulIndex) {
            this.lastConsulIndex = initConsulIndex;
        }

        @Override
        public void run() {
            while (!closed) {
                //Assert.notNull(client.getAgentSelf(), "Current server is not alive.");
                Response<List<HealthService>> response = getDiscoveryServers(lastConsulIndex, WAIT_TIME_SECONDS);
                Long currentIndex = response.getConsulIndex();
                if (currentIndex != null && currentIndex > lastConsulIndex) {
                    lastConsulIndex = currentIndex;
                    refreshDiscoveryServers(response.getValue());
                }
            }
        }
    }

}

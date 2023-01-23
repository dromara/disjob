/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.registry;

import cn.ponfee.scheduler.common.base.ImmutableHashList;
import cn.ponfee.scheduler.common.util.GenericUtils;
import cn.ponfee.scheduler.core.base.Server;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Registry and discovery server.
 *
 * @param <R> the registry server type
 * @param <D> the discovery server type
 * @author Ponfee
 */
public abstract class ServerRegistry<R extends Server, D extends Server> implements Registry<R>, Discovery<D> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final char separator;

    protected final ServerRole registryRole;
    protected final String registryRootPath;

    protected final ServerRole discoveryRole;
    protected final String discoveryRootPath;

    private final DiscoveryServer<D> discoveryServer;

    protected final Set<R> registered = ConcurrentHashMap.newKeySet();

    /**
     * Server registry is whether closed status
     */
    protected final AtomicBoolean closed = new AtomicBoolean(false);

    protected ServerRegistry(String namespace, char separator) {
        this.separator = separator;

        String prefix = prune(namespace, separator);

        this.registryRole = ServerRole.of(GenericUtils.getActualTypeArgument(getClass(), 0));
        this.registryRootPath = prefix + registryRole.key();

        this.discoveryRole = ServerRole.of(GenericUtils.getActualTypeArgument(getClass(), 1));
        this.discoveryRootPath = prefix + discoveryRole.key();

        this.discoveryServer = createDiscoveryServer(discoveryRole);
    }

    /**
     * Returns is connected registry center.
     *
     * @return {@code true} if available
     */
    public abstract boolean isConnected();

    /**
     * Refresh discovery servers.
     *
     * @param servers discovered servers
     */
    protected final void refreshDiscoveredServers(List<D> servers) {
        discoveryServer.refreshServers(servers);
        if (log.isDebugEnabled()) {
            log.debug("Refreshed discovery servers: {} | {}", discoveryRole.name(), servers);
        }
    }

    @Override
    public List<D> getDiscoveredServers(String group) {
        return discoveryServer.getServers(group);
    }

    @Override
    public boolean hasDiscoveredServers() {
        return discoveryServer.hasServers();
    }

    @Override
    public boolean isDiscoveredServer(D server) {
        return discoveryServer.isAlive(server);
    }

    /**
     * Close registry.
     */
    @Override
    public void close() {
        // No-op
    }

    @Override
    public final ServerRole registryRole() {
        return registryRole;
    }

    @Override
    public final ServerRole discoveryRole() {
        return discoveryRole;
    }

    // -------------------------------------------------------------------------------------private method & class definitions

    private static String prune(String namespace, char separator) {
        if (StringUtils.isBlank(namespace)) {
            return "";
        }
        if (namespace.contains(String.valueOf(separator))) {
            throw new IllegalArgumentException("Namespace cannot contains separator symbol '" + separator + "'");
        }
        return namespace.trim() + separator;
    }

    private static <S extends Server> DiscoveryServer<S> createDiscoveryServer(ServerRole discoveryRole) {
        switch (discoveryRole) {
            case WORKER:
                return (DiscoveryServer<S>) new DiscoveryWorker();
            case SUPERVISOR:
                return (DiscoveryServer<S>) new DiscoverySupervisor();
            default:
                throw new UnsupportedOperationException("Unsupported discovery server '" + discoveryRole.name() + "'");
        }
    }

    /**
     * Discovery server.
     * <p>Java language not support class multiple inheritance, so use composite pattern
     *
     * @param <S> the server type
     */
    private static abstract class DiscoveryServer<S extends Server> {

        abstract void refreshServers(List<S> servers);

        abstract List<S> getServers(String group);

        abstract boolean hasServers();

        abstract boolean isAlive(S server);
    }

    /**
     * Discovery supervisor.
     */
    private static final class DiscoverySupervisor extends DiscoveryServer<Supervisor> {
        private volatile ImmutableHashList<String, Supervisor> supervisors = ImmutableHashList.empty();

        @Override
        void refreshServers(List<Supervisor> discoveredSupervisors) {
            this.supervisors = ImmutableHashList.of(discoveredSupervisors, Supervisor::serialize);
        }

        @Override
        List<Supervisor> getServers(String group) {
            Assert.isNull(group, "Discovery supervisor not support grouping.");
            return supervisors.values();
        }

        @Override
        boolean hasServers() {
            return !supervisors.isEmpty();
        }

        @Override
        boolean isAlive(Supervisor supervisor) {
            return supervisors.contains(supervisor);
        }
    }

    /**
     * Discovery worker.
     */
    private static final class DiscoveryWorker extends DiscoveryServer<Worker> {
        private volatile Map<String, ImmutableHashList<String, Worker>> groupedWorkers = Collections.emptyMap();

        @Override
        void refreshServers(List<Worker> discoveredWorkers) {
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
        List<Worker> getServers(String group) {
            Assert.hasText(group, "Discovery worker must be grouping.");
            ImmutableHashList<String, Worker> workers = groupedWorkers.get(group);
            return workers == null ? Collections.emptyList() : workers.values();
        }

        @Override
        boolean hasServers() {
            return !groupedWorkers.isEmpty();
        }

        @Override
        boolean isAlive(Worker worker) {
            ImmutableHashList<String, Worker> workers = groupedWorkers.get(worker.getGroup());
            return workers != null && workers.contains(worker);
        }
    }

}

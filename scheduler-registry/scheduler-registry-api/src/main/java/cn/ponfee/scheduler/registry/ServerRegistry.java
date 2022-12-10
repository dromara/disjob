package cn.ponfee.scheduler.registry;

import cn.ponfee.scheduler.common.base.DoubleListViewer;
import cn.ponfee.scheduler.common.util.GenericUtils;
import cn.ponfee.scheduler.core.base.Server;
import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
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

    // -------------------------------------------------Close
    /**
     * Close registry operation
     */
    protected final AtomicBoolean close = new AtomicBoolean(false);

    /**
     * Closed registry state
     */
    protected volatile boolean closed = false;

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
        log.debug("Refreshed discovery {}", discoveryRole.name());
    }

    @Override
    public List<D> getDiscoveredServers(String group) {
        return discoveryServer.getServers(group);
    }

    /**
     * Close registry.
     */
    @Override
    public void close() {
        discoveryServer.close();
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

    /**
     * Sort for help use server route
     *
     * @param servers    the server list
     * @param sortMapper the sort value mapper
     * @param <S>        server type
     * @return sorted list of servers
     * @see cn.ponfee.scheduler.core.enums.RouteStrategy
     * @see cn.ponfee.scheduler.core.route.ExecutionRouter
     */
    private static <S extends Server, T extends Comparable<T>> List<S> sortServers(List<S> servers, Function<S, T> sortMapper) {
        if (CollectionUtils.isEmpty(servers)) {
            return Collections.emptyList();
        }

        servers.sort(Comparator.comparing(sortMapper::apply));
        return Collections.unmodifiableList(servers);
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

        abstract List<S> getServers(String group);

        abstract void refreshServers(List<S> servers);

        abstract void close();
    }

    /**
     * Discovery supervisor.
     */
    private static final class DiscoverySupervisor extends DiscoveryServer<Supervisor> {
        private volatile List<Supervisor> supervisors = Collections.emptyList();

        @Override
        List<Supervisor> getServers(String group) {
            Assert.isNull(group, "Supervisor not support group, the group argument expect null, but actual is '" + group + "'.");
            return supervisors;
        }

        @Override
        void refreshServers(List<Supervisor> servers) {
            this.supervisors = sortServers(servers, Server::getHost);
        }

        @Override
        void close() {
            this.supervisors = null;
        }
    }

    /**
     * Discovery worker.
     */
    private static final class DiscoveryWorker extends DiscoveryServer<Worker> {
        private volatile Map<String, List<Worker>> groupedWorkers = Collections.emptyMap();
        private volatile List<Worker> allWorkers = Collections.emptyList();

        @Override
        List<Worker> getServers(String group) {
            return group == null ? allWorkers : groupedWorkers.get(group);
        }

        @Override
        void refreshServers(List<Worker> discoveredWorkers) {
            Map<String, List<Worker>> map;
            List<Worker> list;
            if (CollectionUtils.isEmpty(discoveredWorkers)) {
                map = Collections.emptyMap();
                list = Collections.emptyList();
            } else {
                // unnecessary use Collections.unmodifiableMap(map)
                map = discoveredWorkers.stream()
                    .collect(Collectors.groupingBy(Worker::getGroup))
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> sortServers(e.getValue(), Worker::getInstanceId)));
                list = new DoubleListViewer<>(map.values());
            }

            this.groupedWorkers = map;
            this.allWorkers = list;
        }

        @Override
        void close() {
            this.groupedWorkers = null;
            this.allWorkers = null;
        }
    }

}

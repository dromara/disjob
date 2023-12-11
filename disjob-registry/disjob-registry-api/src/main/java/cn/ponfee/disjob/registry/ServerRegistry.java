/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.registry;

import cn.ponfee.disjob.common.util.GenericUtils;
import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.registry.discovery.DiscoveryServer;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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

        this.discoveryServer = DiscoveryServer.of(discoveryRole);
    }

    /**
     * Refresh discovery servers.
     *
     * @param servers discovered servers
     */
    protected final synchronized void refreshDiscoveredServers(List<D> servers) {
        discoveryServer.refreshServers(servers);
        log.debug("Refreshed discovery servers: {} | {}", discoveryRole, servers);
    }

    /**
     * Returns is connected registry center.
     *
     * @return {@code true} if available
     */
    public abstract boolean isConnected();

    @Override
    public final List<D> getDiscoveredServers(String group) {
        return discoveryServer.getServers(group);
    }

    @Override
    public final boolean hasDiscoveredServers() {
        return discoveryServer.hasServers();
    }

    @Override
    public final boolean isDiscoveredServer(D server) {
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

    protected final List<R> deserializeRegistryServers(List<String> list) {
        return deserializeRegistryServers(list, Function.identity());
    }

    protected final <T> List<R> deserializeRegistryServers(List<T> list, Function<T, String> function) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream()
            .filter(Objects::nonNull)
            .map(function)
            .filter(StringUtils::isNotBlank)
            .<R>map(registryRole::deserialize)
            .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------------------private method

    private static String prune(String namespace, char separator) {
        if (StringUtils.isBlank(namespace)) {
            return "";
        }
        if (namespace.contains(String.valueOf(separator))) {
            throw new IllegalArgumentException("Namespace cannot contains separator symbol '" + separator + "'");
        }
        return namespace.trim() + separator;
    }

}

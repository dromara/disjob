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

package cn.ponfee.disjob.registry;

import cn.ponfee.disjob.common.concurrent.TripState;
import cn.ponfee.disjob.common.util.GenericUtils;
import cn.ponfee.disjob.common.util.Strings;
import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.core.enums.RegistryEventType;
import cn.ponfee.disjob.registry.discovery.ServerDiscovery;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
    private final ServerDiscovery<D, R> serverDiscovery;

    /**
     * Registered servers.
     */
    protected final Set<R> registered = ConcurrentHashMap.newKeySet();

    /**
     * Server registry state
     */
    protected final TripState state = TripState.createStarted();

    protected ServerRegistry(AbstractRegistryProperties config, RestTemplate restTemplate, char separator) {
        this.separator = separator;

        String prefix = prune(config.getNamespace(), separator);

        this.registryRole = ServerRole.of(GenericUtils.getActualTypeArgument(getClass(), 0));
        this.registryRootPath = prefix + registryRole.key();

        this.discoveryRole = ServerRole.of(GenericUtils.getActualTypeArgument(getClass(), 1));
        this.discoveryRootPath = prefix + discoveryRole.key();

        this.serverDiscovery = ServerDiscovery.of(discoveryRole, restTemplate);
    }

    /**
     * Returns is connected registry center.
     *
     * @return {@code true} if available
     */
    public abstract boolean isConnected();

    @Override
    public final List<D> getDiscoveredServers(String group) {
        return serverDiscovery.getServers(group);
    }

    @Override
    public final boolean hasDiscoveredServers() {
        return serverDiscovery.hasServers();
    }

    @Override
    public final boolean isDiscoveredServer(D server) {
        return serverDiscovery.isAlive(server);
    }

    /**
     * Close registry.
     */
    @Override
    public void close() {
        registered.clear();
    }

    @Override
    public final ServerRole registryRole() {
        return registryRole;
    }

    @Override
    public final ServerRole discoveryRole() {
        return discoveryRole;
    }

    protected void publishServerEvent(RegistryEventType eventType, R rServer) {
        log.info("Publish server event: {}, {}", eventType, rServer);
        serverDiscovery.notifyServers(eventType, rServer);
    }

    @Override
    public final void subscribeServerEvent(RegistryEventType eventType, D dServer) {
        log.info("Subscribe server event: {}, {}", eventType, dServer);
        serverDiscovery.updateServers(eventType, dServer);
    }

    /**
     * Refresh discovery servers.
     *
     * @param list the list
     */
    protected final void refreshDiscoveryServers(List<String> list) {
        List<D> servers = deserializeServers(list, discoveryRole);
        serverDiscovery.refreshServers(servers);
        if (servers.isEmpty()) {
            log.warn("Not discovered available {}", discoveryRole);
        }
    }

    protected final <S extends Server> List<S> deserializeServers(List<String> list, ServerRole serverRole) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream()
            .<S>map(e -> deserializeServer(e, serverRole))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    protected final <S extends Server> S deserializeServer(String server, ServerRole serverRole) {
        if (StringUtils.isBlank(server)) {
            return null;
        }
        try {
            return serverRole.deserialize(server);
        } catch (Throwable t) {
            log.error("Deserialize server failed: {}, {}, {}", serverRole, server, t.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------------------private method

    private static String prune(String namespace, char separator) {
        if (StringUtils.isEmpty(namespace)) {
            return "";
        }
        if (Strings.containsCharOrWhitespace(namespace, separator)) {
            throw new IllegalArgumentException("Namespace cannot contains separator symbol '" + separator + "'");
        }
        return namespace + separator;
    }

}

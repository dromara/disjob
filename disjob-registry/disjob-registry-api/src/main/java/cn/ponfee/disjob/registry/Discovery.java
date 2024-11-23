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

import cn.ponfee.disjob.core.base.RegistryEventType;
import cn.ponfee.disjob.core.base.Server;

import java.io.Closeable;
import java.util.List;

/**
 * Discovery server.
 *
 * @param <D> the discovery server type
 * @author Ponfee
 */
public interface Discovery<D extends Server> extends Closeable {

    /**
     * Discover servers
     *
     * @throws Throwable if occur error
     */
    default void discoverServers() throws Throwable {
        // default do nothing
    }

    /**
     * Gets grouped alive discovered servers.
     *
     * @param group the discovered interested group
     * @return list of grouped alive discovered servers
     */
    List<D> getDiscoveredServers(String group);

    /**
     * Returns is whether discovered any server.
     *
     * @return {@code true} if discovered at least one server.
     */
    boolean hasDiscoveredServers();

    /**
     * Returns a boolean for the server is whether alive.
     *
     * @param server the server
     * @return {@code true} if is alive
     */
    boolean isDiscoveredServer(D server);

    /**
     * Subscribe server changed event
     *
     * @param eventType the registry event type
     * @param server    the discovery server
     */
    void subscribeServerChanged(RegistryEventType eventType, D server);

    /**
     * Returns discovery server role.
     *
     * @return discovery server role
     */
    ServerRole discoveryRole();

    /**
     * Close discovery.
     */
    @Override
    void close();
}

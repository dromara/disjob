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

import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.core.enums.RegistryEventType;

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
     * Gets alive discovered servers of group.
     *
     * @param group the group
     * @return list alive discovered servers of group
     */
    List<D> getAliveServers(String group);

    /**
     * Returns has alive server.
     *
     * @return {@code true} if has least one server alive.
     */
    boolean hasAliveServer();

    /**
     * Returns the server is whether alive.
     *
     * @param server the server
     * @return {@code true} if is alive
     */
    boolean isAliveServer(D server);

    /**
     * Subscribe server event
     *
     * @param eventType the registry event type
     * @param server    the discovery server
     */
    void subscribeServerEvent(RegistryEventType eventType, D server);

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

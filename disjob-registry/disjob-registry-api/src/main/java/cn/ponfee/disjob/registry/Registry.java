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

import java.io.Closeable;
import java.util.List;

/**
 * Server registry.
 *
 * @param <R> the registry server type
 * @author Ponfee
 */
public interface Registry<R extends Server> extends Closeable {

    /**
     * Register the server to cluster.
     *
     * @param server the registering server
     */
    void register(R server);

    /**
     * Deregister the server from cluster.
     *
     * @param server the registered server
     */
    void deregister(R server);

    /**
     * Returns registry server role.
     *
     * @return registry server role
     */
    ServerRole registryRole();

    /**
     * Gets alive registered servers.
     *
     * @return list of alive registered servers
     */
    List<R> getRegisteredServers();

    /**
     * Close registry.
     */
    @Override
    void close();
}

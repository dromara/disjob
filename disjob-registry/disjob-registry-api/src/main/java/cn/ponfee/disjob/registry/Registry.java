/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

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
     * @throws Exception if occur exception
     */
    List<R> getRegisteredServers() throws Exception;

    /**
     * Close registry.
     */
    @Override
    void close();
}

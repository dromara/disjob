/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.registry;

import cn.ponfee.scheduler.core.base.Server;

/**
 * Server registry.
 *
 * @param <R> the registry server type
 * @author Ponfee
 */
public interface Registry<R extends Server> extends AutoCloseable {

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
     * Close registry.
     */
    @Override
    void close();
}

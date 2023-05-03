/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.registry;

import cn.ponfee.disjob.core.base.Server;

import java.util.List;

/**
 * Discovery server.
 *
 * @param <D> the discovery server type
 * @author Ponfee
 */
public interface Discovery<D extends Server> extends AutoCloseable {

    /**
     * Gets grouped alive discovered servers.
     *
     * @param group the discovered interested group
     * @return list of grouped alive discovered servers
     */
    List<D> getDiscoveredServers(String group);

    /**
     * Returns is whether discovered any server.
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

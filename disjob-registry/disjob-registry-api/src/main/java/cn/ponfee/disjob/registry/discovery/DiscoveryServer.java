/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.registry.discovery;

import cn.ponfee.disjob.core.base.Server;
import cn.ponfee.disjob.registry.ServerRole;

import java.util.List;

/**
 * Discovery server.
 *
 * @author Ponfee
 */
public interface DiscoveryServer<S extends Server> {

    void refreshServers(List<S> servers);

    List<S> getServers(String group);

    boolean hasServers();

    boolean isAlive(S server);

    static <S extends Server> DiscoveryServer<S> of(ServerRole discoveryRole) {
        switch (discoveryRole) {
            case WORKER:
                return (DiscoveryServer<S>) new DiscoveryWorker();
            case SUPERVISOR:
                return (DiscoveryServer<S>) new DiscoverySupervisor();
            default:
                throw new UnsupportedOperationException("Unsupported discovery server '" + discoveryRole + "'");
        }
    }

}

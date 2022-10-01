package cn.ponfee.scheduler.registry;

import cn.ponfee.scheduler.core.base.Server;

import java.util.List;

/**
 * Discovery server.
 *
 * @param <D> the discovery server type
 * @author Ponfee
 */
public interface Discovery<D extends Server> {

    /**
     * Gets all alive servers.
     *
     * @return all alive servers
     */
    default List<D> getServers() {
        return getServers(null);
    }

    /**
     * Gets grouped alive servers.
     *
     * @param group the discovered interested group
     * @return list of grouped alive servers
     */
    List<D> getServers(String group);

    /**
     * Returns a boolean for the server is whether alive.
     *
     * @param server the server
     * @return {@code true} if is alive
     */
    boolean isAlive(D server);

    /**
     * Returns discovery role name.
     *
     * @return discovery role name
     */
    String discoveryRole();

}

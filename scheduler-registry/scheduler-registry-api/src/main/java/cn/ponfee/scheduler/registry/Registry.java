package cn.ponfee.scheduler.registry;

import cn.ponfee.scheduler.core.base.Server;

/**
 * Server registry.
 *
 * @param <R> the registry server type
 * @author Ponfee
 */
public interface Registry<R extends Server> {

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
     * Returns registry role name.
     * 
     * @return registry role name
     */
    String registryRole();
}

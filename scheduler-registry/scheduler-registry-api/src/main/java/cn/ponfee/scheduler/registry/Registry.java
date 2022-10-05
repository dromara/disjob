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
     * Returns registry server role.
     * 
     * @return registry server role
     */
    ServerRole registryRole();

    /**
     * Publish the server register and deregister event.
     *
     * @param server the server
     * @param event  the event
     */
    default void publish(R server, RegistryEvent event) {
        // No-op
    }
}

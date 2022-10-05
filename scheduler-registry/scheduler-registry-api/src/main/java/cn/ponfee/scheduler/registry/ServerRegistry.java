package cn.ponfee.scheduler.registry;

import cn.ponfee.scheduler.common.concurrent.ConcurrentHashSet;
import cn.ponfee.scheduler.common.util.GenericUtils;
import cn.ponfee.scheduler.core.base.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Registry and discovery server.
 *
 * @param <R> the registry server type
 * @param <D> the discovery server type
 * @author Ponfee
 */
public abstract class ServerRegistry<R extends Server, D extends Server> implements Registry<R>, Discovery<D>, AutoCloseable {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final ServerRole registryRole;
    protected final ServerRole discoveryRole;

    protected final Set<R> registered = new ConcurrentHashSet<>();

    // -------------------------------------------------Close
    /**
     * Close registry operation
     */
    protected final AtomicBoolean close = new AtomicBoolean(false);

    /**
     * Closed registry state
     */
    protected volatile boolean closed = false;

    protected ServerRegistry() {
        this.registryRole  = ServerRole.of(GenericUtils.getActualTypeArgument(getClass(), 0));
        this.discoveryRole = ServerRole.of(GenericUtils.getActualTypeArgument(getClass(), 1));
    }

    /**
     * Close registry.
     */
    @Override
    public void close() {
        // No-op
    }

    @Override
    public ServerRole registryRole() {
        return registryRole;
    }

    @Override
    public ServerRole discoveryRole() {
        return discoveryRole;
    }

}

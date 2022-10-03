package cn.ponfee.scheduler.registry;

import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;

/**
 * Worker registry and discovery supervisor.
 *
 * @author Ponfee
 */
public interface WorkerRegistry extends Registry<Worker>, Discovery<Supervisor>, AutoCloseable {

    /**
     * Close registry.
     */
    @Override
    void close();
}

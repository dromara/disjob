package cn.ponfee.scheduler.registry;

import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;

/**
 * Supervisor registry and discovery worker.
 *
 * @author Ponfee
 */
public interface SupervisorRegistry extends Registry<Supervisor>, Discovery<Worker>, AutoCloseable {

    /**
     * Close registry.
     */
    @Override
    void close();
}

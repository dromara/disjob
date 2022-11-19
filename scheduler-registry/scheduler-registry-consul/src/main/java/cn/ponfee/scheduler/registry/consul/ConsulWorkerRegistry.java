package cn.ponfee.scheduler.registry.consul;

import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.WorkerRegistry;

/**
 * Registry worker based consul.
 *
 * @author Ponfee
 */
public class ConsulWorkerRegistry extends ConsulServerRegistry<Worker, Supervisor> implements WorkerRegistry {

    public ConsulWorkerRegistry(String host, int port, String token) {
        super(host, port, token);
    }

}

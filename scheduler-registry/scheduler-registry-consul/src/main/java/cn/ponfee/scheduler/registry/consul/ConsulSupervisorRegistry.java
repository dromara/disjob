package cn.ponfee.scheduler.registry.consul;

import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.SupervisorRegistry;

/**
 * Registry supervisor based consul.
 *
 * @author Ponfee
 */
public class ConsulSupervisorRegistry extends ConsulServerRegistry<Supervisor, Worker> implements SupervisorRegistry {

    public ConsulSupervisorRegistry(String host, int port, String token) {
        super(host, port, token);
    }

}

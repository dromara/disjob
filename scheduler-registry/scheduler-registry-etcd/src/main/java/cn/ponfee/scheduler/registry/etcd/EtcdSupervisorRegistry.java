package cn.ponfee.scheduler.registry.etcd;

import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.SupervisorRegistry;
import cn.ponfee.scheduler.registry.etcd.configuration.EtcdProperties;

/**
 * Registry supervisor based Etcd.
 *
 * @author Ponfee
 */
public class EtcdSupervisorRegistry extends EtcdServerRegistry<Supervisor, Worker> implements SupervisorRegistry {

    public EtcdSupervisorRegistry(String namespace, EtcdProperties properties) {
        super(namespace, properties);
    }

}

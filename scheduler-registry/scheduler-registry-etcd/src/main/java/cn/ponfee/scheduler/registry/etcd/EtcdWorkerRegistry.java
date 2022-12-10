package cn.ponfee.scheduler.registry.etcd;

import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.WorkerRegistry;
import cn.ponfee.scheduler.registry.etcd.configuration.EtcdProperties;

/**
 * Registry worker based Etcd.
 *
 * @author Ponfee
 */
public class EtcdWorkerRegistry extends EtcdServerRegistry<Worker, Supervisor> implements WorkerRegistry {

    public EtcdWorkerRegistry(String namespace, EtcdProperties properties) {
        super(namespace, properties);
    }

}

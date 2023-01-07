/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.registry.zookeeper;

import cn.ponfee.scheduler.core.base.Supervisor;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.registry.WorkerRegistry;
import cn.ponfee.scheduler.registry.zookeeper.configuration.ZookeeperProperties;

/**
 * Registry worker based zookeeper.
 *
 * @author Ponfee
 */
public class ZookeeperWorkerRegistry extends ZookeeperServerRegistry<Worker, Supervisor> implements WorkerRegistry {

    public ZookeeperWorkerRegistry(String namespace, ZookeeperProperties props) {
        super(namespace, props);
    }

}

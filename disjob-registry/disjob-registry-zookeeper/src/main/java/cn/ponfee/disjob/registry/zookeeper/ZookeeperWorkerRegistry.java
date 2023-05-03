/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.registry.zookeeper;

import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.registry.zookeeper.configuration.ZookeeperRegistryProperties;

/**
 * Registry worker based zookeeper.
 *
 * @author Ponfee
 */
public class ZookeeperWorkerRegistry extends ZookeeperServerRegistry<Worker, Supervisor> implements WorkerRegistry {

    public ZookeeperWorkerRegistry(ZookeeperRegistryProperties config) {
        super(config);
    }

}

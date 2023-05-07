/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.registry.nacos;

import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.registry.nacos.configuration.NacosRegistryProperties;

/**
 * Registry worker based nacos.
 *
 * @author Ponfee
 */
public class NacosWorkerRegistry extends NacosServerRegistry<Worker, Supervisor> implements WorkerRegistry {

    public NacosWorkerRegistry(NacosRegistryProperties config) {
        super(config);
    }

}
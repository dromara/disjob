/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.base;

import cn.ponfee.disjob.core.base.WorkerRpcService;
import cn.ponfee.disjob.registry.DiscoveryRestProxy;
import org.junit.jupiter.api.Test;

/**
 * DiscoveryRestProxyTest
 *
 * @author Ponfee
 */
public class DiscoveryRestProxyTest {

    @Test
    public void testGroupedServer() {
        /*
        DiscoveryRestTemplate<Worker> discoveryRestTemplate = DiscoveryRestTemplate.<Worker>builder()
            .httpConnectTimeout(2000)
            .httpReadTimeout(5000)
            .retryMaxCount(3)
            .retryBackoffPeriod(5000)
            .discoveryServer(new ConsulSupervisorRegistry(new ConsulRegistryProperties()))
            .build();
        WorkerRpcService workerRpcService = DiscoveryRestProxy.create(true, WorkerRpcService.class, discoveryRestTemplate);
        */

        WorkerRpcService workerRpcService = DiscoveryRestProxy.create(true, WorkerRpcService.class, null);
        ((DiscoveryRestProxy.GroupedServer) workerRpcService).group("test");
    }

}

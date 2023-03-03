/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.test.job.registry;

import cn.ponfee.scheduler.core.base.WorkerService;
import cn.ponfee.scheduler.registry.DiscoveryRestProxy;
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
            .connectTimeout(2000)
            .readTimeout(2000)
            .maxRetryTimes(3)
            .objectMapper(Jsons.createObjectMapper(JsonInclude.Include.NON_NULL))
            .discoveryServer(new ConsulSupervisorRegistry(new ConsulRegistryProperties()))
            .build();
        WorkerService remoteWorkerService = DiscoveryRestProxy.create(WorkerService.class, discoveryRestTemplate);
        */

        WorkerService remoteWorkerService = DiscoveryRestProxy.create(true, WorkerService.class, null);
        ((DiscoveryRestProxy.GroupedServer)remoteWorkerService).group("test");
    }

}

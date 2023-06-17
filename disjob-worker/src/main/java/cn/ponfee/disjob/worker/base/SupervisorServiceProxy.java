/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.worker.base;

import cn.ponfee.disjob.common.base.RetryInvocationHandler;
import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.SupervisorService;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Objects;

/**
 * Supervisor service proxy
 *
 * @author Ponfee
 */
public class SupervisorServiceProxy {

    public static SupervisorService newRetryProxyIfLocal(SupervisorService service, RetryProperties config) {
        Objects.requireNonNull(service, "Supervisor service cannot be null.");
        Objects.requireNonNull(config, "Retry config cannot be null.").check();

        if (!service.getClass().getName().startsWith(JobConstants.JOB_MANAGER_CLASS_NAME)) {
            // non-local supervisor service: com.sun.proxy.$Proxy131
            return service;
        }

        // Spring bean proxy: cn.ponfee.disjob.supervisor.manager.DistributedJobManager$$EnhancerBySpringCGLIB$$c16a8c35
        ClassLoader classLoader = service.getClass().getClassLoader();
        Class<?>[] interfaces = {SupervisorService.class};
        InvocationHandler ih = new RetryInvocationHandler(service, config.getMaxCount(), config.getBackoffPeriod());
        return (SupervisorService) Proxy.newProxyInstance(classLoader, interfaces, ih);
    }

}

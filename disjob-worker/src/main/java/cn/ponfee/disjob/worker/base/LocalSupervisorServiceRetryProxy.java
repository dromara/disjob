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
import cn.ponfee.disjob.core.base.SupervisorService;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Objects;

/**
 * Local supervisor service proxy with retry
 *
 * @author Ponfee
 */
public class LocalSupervisorServiceRetryProxy {

    public static SupervisorService newRetryProxyIfLocal(SupervisorService service, int retryMaxCount, int retryBackoffPeriod) {
        Objects.requireNonNull(service, "Supervisor service cannot be null.");

        if (!service.getClass().getName().startsWith(JobConstants.JOB_MANAGER_CLASS_NAME)) {
            // non-local supervisor service: com.sun.proxy.$Proxy131
            return service;
        }

        // Spring bean proxy: cn.ponfee.disjob.supervisor.manager.DistributedJobManager$$EnhancerBySpringCGLIB$$c16a8c35
        ClassLoader classLoader = service.getClass().getClassLoader();
        Class<?>[] interfaces = {SupervisorService.class};
        InvocationHandler invocationHandler = new RetryInvocationHandler(service, retryMaxCount, retryBackoffPeriod);
        return (SupervisorService) Proxy.newProxyInstance(classLoader, interfaces, invocationHandler);
    }

}

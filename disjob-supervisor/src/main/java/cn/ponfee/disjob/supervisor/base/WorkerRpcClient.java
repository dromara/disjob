/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.base;

import cn.ponfee.disjob.core.base.*;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.handle.JobHandlerUtils;
import cn.ponfee.disjob.core.handle.SplitTask;
import cn.ponfee.disjob.core.param.worker.ConfigureWorkerParam;
import cn.ponfee.disjob.core.param.worker.GetMetricsParam;
import cn.ponfee.disjob.core.param.worker.JobHandlerParam;
import cn.ponfee.disjob.registry.DiscoveryRestProxy;
import cn.ponfee.disjob.registry.DiscoveryRestTemplate;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.supervisor.application.SchedGroupService;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.Nullable;
import java.util.List;

/**
 * WorkerRpcService client
 *
 * @author Ponfee
 */
public class WorkerRpcClient {

    private final Worker.Current currentWorker;
    private final WorkerRpcService local;
    private final WorkerRpcService remote;

    public WorkerRpcClient(HttpProperties httpProperties,
                           RetryProperties retryProperties,
                           SupervisorRegistry supervisorRegistry,
                           @Nullable Worker.Current currentWorker,
                           @Nullable ObjectMapper objectMapper) {
        httpProperties.check();
        retryProperties.check();
        DiscoveryRestTemplate<Worker> discoveryRestTemplate = DiscoveryRestTemplate.<Worker>builder()
            .httpConnectTimeout(httpProperties.getConnectTimeout())
            .httpReadTimeout(httpProperties.getReadTimeout())
            .retryMaxCount(retryProperties.getMaxCount())
            .retryBackoffPeriod(retryProperties.getBackoffPeriod())
            .objectMapper(objectMapper)
            .discoveryServer(supervisorRegistry)
            .build();

        this.currentWorker = currentWorker;
        this.local = WorkerRpcLocal.INSTANCE;
        this.remote = DiscoveryRestProxy.create(true, WorkerRpcService.class, discoveryRestTemplate);
    }

    public void verify(JobHandlerParam param) throws JobException {
        param.setSupervisorToken(SchedGroupService.getGroup(param.getGroup()).getSupervisorToken());
        grouped(param.getGroup()).verify(param);
    }

    public List<SplitTask> split(JobHandlerParam param) throws JobException {
        param.setSupervisorToken(SchedGroupService.getGroup(param.getGroup()).getSupervisorToken());
        return grouped(param.getGroup()).split(param);
    }

    // ------------------------------------------------------------private methods & class

    private WorkerRpcService grouped(String group) {
        if (currentWorker != null && currentWorker.matchesGroup(group)) {
            return local;
        } else {
            ((DiscoveryRestProxy.GroupedServer) remote).group(group);
            return remote;
        }
    }

    private static class WorkerRpcLocal implements WorkerRpcService {
        private static final WorkerRpcLocal INSTANCE = new WorkerRpcLocal();

        @Override
        public void verify(JobHandlerParam param) throws JobException {
            JobHandlerUtils.verify(param);
        }

        @Override
        public List<SplitTask> split(JobHandlerParam param) throws JobException {
            return JobHandlerUtils.split(param);
        }

        @Override
        public WorkerMetrics metrics(GetMetricsParam param) {
            throw new UnsupportedOperationException("Unsupported local metrics.");
        }

        @Override
        public void configureWorker(ConfigureWorkerParam param) {
            throw new UnsupportedOperationException("Unsupported local configureWorker.");
        }
    }

}

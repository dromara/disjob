/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.base;

import cn.ponfee.disjob.core.base.HttpProperties;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.base.WorkerCoreRpcService;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.handle.JobHandlerUtils;
import cn.ponfee.disjob.core.handle.SplitTask;
import cn.ponfee.disjob.core.param.worker.JobHandlerParam;
import cn.ponfee.disjob.registry.DiscoveryRestProxy;
import cn.ponfee.disjob.registry.DiscoveryRestTemplate;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.supervisor.service.SchedGroupManager;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.Nullable;
import java.util.List;

/**
 * WorkerCoreRpcService client
 *
 * @author Ponfee
 */
public class WorkerCoreRpcClient {

    private final Worker currentWorker;
    private final WorkerCoreRpcService local;
    private final WorkerCoreRpcService remote;

    public WorkerCoreRpcClient(HttpProperties httpProperties,
                               RetryProperties retryProperties,
                               SupervisorRegistry supervisorRegistry,
                               @Nullable Worker currentWorker,
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
        this.local = WorkerCoreRpcLocal.INSTANCE;
        this.remote = DiscoveryRestProxy.create(true, WorkerCoreRpcService.class, discoveryRestTemplate);
    }

    public void verify(JobHandlerParam param) throws JobException {
        param.setSupervisorToken(getSupervisorToken(param.getJobGroup()));
        grouped(param.getJobGroup()).verify(param);
    }

    public List<SplitTask> split(JobHandlerParam param) throws JobException {
        param.setSupervisorToken(getSupervisorToken(param.getJobGroup()));
        return grouped(param.getJobGroup()).split(param);
    }

    // ------------------------------------------------------------private methods & class

    private WorkerCoreRpcService grouped(String group) {
        if (currentWorker != null && currentWorker.matchesGroup(group)) {
            return local;
        } else {
            ((DiscoveryRestProxy.GroupedServer) remote).group(group);
            return remote;
        }
    }

    private String getSupervisorToken(String group) {
        SchedGroupManager.DisjobGroup disjobGroup = SchedGroupManager.getDisjobGroup(group);
        if (disjobGroup == null) {
            throw new IllegalStateException("Not found worker group: " + group);
        }
        return disjobGroup.getSupervisorToken();
    }

    private static class WorkerCoreRpcLocal implements WorkerCoreRpcService {
        private static final WorkerCoreRpcLocal INSTANCE = new WorkerCoreRpcLocal();

        @Override
        public void verify(JobHandlerParam param) throws JobException {
            JobHandlerUtils.verify(param);
        }

        @Override
        public List<SplitTask> split(JobHandlerParam param) throws JobException {
            return JobHandlerUtils.split(param);
        }
    }

}

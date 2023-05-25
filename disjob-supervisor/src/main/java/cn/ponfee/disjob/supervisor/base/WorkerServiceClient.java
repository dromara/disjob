/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.base;

import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.base.WorkerService;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.handle.JobHandlerUtils;
import cn.ponfee.disjob.core.handle.SplitTask;
import cn.ponfee.disjob.core.param.JobHandlerParam;
import cn.ponfee.disjob.registry.DiscoveryRestProxy;

import java.util.List;

/**
 * Worker service client
 *
 * @author Ponfee
 */
public class WorkerServiceClient {

    private static final WorkerService LOCAL_WORKER_SERVICE = new WorkerService() {
        @Override
        public void verify(JobHandlerParam param) throws JobException {
            JobHandlerUtils.verify(param);
        }

        @Override
        public List<SplitTask> split(JobHandlerParam param) throws JobException {
            return JobHandlerUtils.split(param);
        }
    };

    private final WorkerService remoteWorkerService;
    private final String currentGroup;

    public WorkerServiceClient(WorkerService remoteWorkerService, Worker currentWorker) {
        this.remoteWorkerService = remoteWorkerService;
        this.currentGroup = currentWorker == null ? null : currentWorker.getGroup();
    }

    public void verify(JobHandlerParam param) throws JobException {
        workerService(param.getJobGroup()).verify(param);
    }

    public List<SplitTask> split(JobHandlerParam param) throws JobException {
        return workerService(param.getJobGroup()).split(param);
    }

    // ------------------------------------------------------------private methods

    private WorkerService workerService(String group) {
        if ((remoteWorkerService == null || group.equals(currentGroup))) {
            return LOCAL_WORKER_SERVICE;
        }

        if (remoteWorkerService instanceof DiscoveryRestProxy.GroupedServer) {
            ((DiscoveryRestProxy.GroupedServer) remoteWorkerService).group(group);
        }
        return remoteWorkerService;
    }

}

/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.base;

import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.base.WorkerService;
import cn.ponfee.scheduler.core.exception.JobException;
import cn.ponfee.scheduler.core.handle.JobHandlerUtils;
import cn.ponfee.scheduler.core.handle.SplitTask;
import cn.ponfee.scheduler.registry.DiscoveryRestProxy;

import java.util.List;

/**
 * Worker service client
 *
 * @author Ponfee
 */
public class WorkerServiceClient {

    private static final WorkerService LOCAL_WORKER_SERVICE = new WorkerService() {
        @Override
        public boolean verify(String jobHandler, String jobParam) {
            return JobHandlerUtils.verify(jobHandler, jobParam);
        }

        @Override
        public List<SplitTask> split(String jobHandler, String jobParam) throws JobException {
            return JobHandlerUtils.split(jobHandler, jobParam);
        }
    };

    private final WorkerService remoteWorkerService;
    private final String currentGroup;

    public WorkerServiceClient() {
        this(null, null);
    }

    public WorkerServiceClient(WorkerService remoteWorkerService, Worker currentWorker) {
        this.remoteWorkerService = remoteWorkerService;
        this.currentGroup = currentWorker == null ? null : currentWorker.getGroup();
    }

    public boolean verify(String group, String jobHandler, String jobParam) {
        return get(group).verify(jobHandler, jobParam);
    }

    public List<SplitTask> split(String group, String jobHandler, String jobParam) throws JobException {
        return get(group).split(jobHandler, jobParam);
    }

    // ------------------------------------------------------------private methods

    private WorkerService get(String group) {
        if ((remoteWorkerService == null || group.equals(currentGroup))) {
            return LOCAL_WORKER_SERVICE;
        } else {
            if (remoteWorkerService instanceof DiscoveryRestProxy.ImplantGroup) {
                ((DiscoveryRestProxy.ImplantGroup) remoteWorkerService).group(group);
            }
            return remoteWorkerService;
        }
    }

}

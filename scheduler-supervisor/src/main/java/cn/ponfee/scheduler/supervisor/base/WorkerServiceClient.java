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

import java.util.List;
import java.util.Objects;

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

    private final Worker currentWorker;
    private final WorkerService remoteWorkerService;

    public WorkerServiceClient(Worker currentWorker, WorkerService remoteWorkerService) {
        this.currentWorker = currentWorker;
        this.remoteWorkerService = Objects.requireNonNull(remoteWorkerService);
    }

    public boolean verify(String group, String jobHandler, String jobParam) {
        return get(group).verify(jobHandler, jobParam);
    }

    public List<SplitTask> split(String group, String jobHandler, String jobParam) throws JobException {
        return get(group).split(jobHandler, jobParam);
    }

    // ------------------------------------------------------------private methods

    private WorkerService get(String group) {
        return (currentWorker != null && group.equals(currentWorker.getGroup()))
            ? LOCAL_WORKER_SERVICE
            : remoteWorkerService;
    }

}

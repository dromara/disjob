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

    private static final WorkerService LOCAL = new Local();

    private final Worker worker;
    private final WorkerService remote;

    public WorkerServiceClient(Worker worker, WorkerService remote) {
        this.worker = worker;
        this.remote = Objects.requireNonNull(remote);
    }

    public boolean verify(String group, String jobHandler, String jobParam) {
        if (worker != null && group.equals(worker.getGroup())) {
            // run local to verify
            return LOCAL.verify(jobHandler, jobParam);
        } else {
            // call remote to split
            return remote.verify(jobHandler, jobParam);
        }
    }

    public List<SplitTask> split(String group, String jobHandler, String jobParam) throws JobException {
        if (worker != null && group.equals(worker.getGroup())) {
            // run local to split
            return LOCAL.split(jobHandler, jobParam);
        } else {
            // call remote to split
            return remote.split(jobHandler, jobParam);
        }
    }

    // ------------------------------------------------------------static class definition

    private static class Local implements WorkerService {
        @Override
        public boolean verify(String jobHandler, String jobParam) {
            return JobHandlerUtils.verify(jobHandler, jobParam);
        }

        @Override
        public List<SplitTask> split(String jobHandler, String jobParam) throws JobException {
            return JobHandlerUtils.split(jobHandler, jobParam);
        }
    }

}

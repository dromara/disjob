/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.base;

import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.base.WorkerCoreRpcService;
import cn.ponfee.disjob.core.exception.JobCheckedException;
import cn.ponfee.disjob.core.handle.JobHandlerUtils;
import cn.ponfee.disjob.core.handle.SplitTask;
import cn.ponfee.disjob.core.param.JobHandlerParam;
import cn.ponfee.disjob.registry.DiscoveryRestProxy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Worker service client
 *
 * @author Ponfee
 */
public class WorkerServiceClient {

    private static final WorkerCoreRpcService WORKER_CORE_RPC_LOCAL = new WorkerCoreRpcLocal();

    private final WorkerCoreRpcService workerCoreRpcClient;
    private final Worker currentWorker;

    public WorkerServiceClient(@Nonnull WorkerCoreRpcService workerCoreRpcClient,
                               @Nullable Worker currentWorker) {
        this.workerCoreRpcClient = Objects.requireNonNull(workerCoreRpcClient);
        this.currentWorker = currentWorker;
    }

    public void verify(JobHandlerParam param) throws JobCheckedException {
        get(param.getJobGroup()).verify(param);
    }

    public List<SplitTask> split(JobHandlerParam param) throws JobCheckedException {
        return get(param.getJobGroup()).split(param);
    }

    // ------------------------------------------------------------private methods

    private WorkerCoreRpcService get(String group) {
        if (currentWorker != null && currentWorker.matchesGroup(group)) {
            return WORKER_CORE_RPC_LOCAL;
        }

        if (workerCoreRpcClient instanceof DiscoveryRestProxy.GroupedServer) {
            ((DiscoveryRestProxy.GroupedServer) workerCoreRpcClient).group(group);
        }
        return workerCoreRpcClient;
    }

    private static class WorkerCoreRpcLocal implements WorkerCoreRpcService {

        @Override
        public void verify(JobHandlerParam param) throws JobCheckedException {
            JobHandlerUtils.verify(param);
        }

        @Override
        public List<SplitTask> split(JobHandlerParam param) throws JobCheckedException {
            return JobHandlerUtils.split(param);
        }
    }

}

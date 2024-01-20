/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.worker.provider;

import cn.ponfee.disjob.common.spring.RpcController;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.base.WorkerMetrics;
import cn.ponfee.disjob.core.base.WorkerRpcService;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.handle.JobHandlerUtils;
import cn.ponfee.disjob.core.handle.SplitTask;
import cn.ponfee.disjob.core.param.worker.ConfigureWorkerParam;
import cn.ponfee.disjob.core.param.worker.ConfigureWorkerParam.Action;
import cn.ponfee.disjob.core.param.worker.GetMetricsParam;
import cn.ponfee.disjob.core.param.worker.JobHandlerParam;
import cn.ponfee.disjob.registry.WorkerRegistry;
import cn.ponfee.disjob.worker.base.WorkerConfigurator;

import java.util.List;

/**
 * Worker rpc service provider.
 *
 * @author Ponfee
 */
public class WorkerRpcProvider implements WorkerRpcService, RpcController {

    private final Worker.Current currentWork;
    private final WorkerRegistry workerRegistry;

    public WorkerRpcProvider(Worker.Current currentWork,
                             WorkerRegistry workerRegistry) {
        this.currentWork = currentWork;
        this.workerRegistry = workerRegistry;
    }

    @Override
    public void verify(JobHandlerParam param) throws JobException {
        currentWork.authenticate(param);
        JobHandlerUtils.verify(param);
    }

    @Override
    public List<SplitTask> split(JobHandlerParam param) throws JobException {
        currentWork.authenticate(param);
        return JobHandlerUtils.split(param);
    }

    @Override
    public WorkerMetrics metrics(GetMetricsParam param) {
        currentWork.authenticate(param);
        return WorkerConfigurator.metrics();
    }

    @Override
    public void configureWorker(ConfigureWorkerParam param) {
        currentWork.authenticate(param);
        Action action = param.getAction();
        if (action == Action.MODIFY_MAXIMUM_POOL_SIZE) {
            Integer maximumPoolSize = action.parse(param.getData());
            WorkerConfigurator.modifyMaximumPoolSize(maximumPoolSize);

        } else if (action == Action.CLEAR_TASK_QUEUE) {
            WorkerConfigurator.clearTaskQueue();

        } else if (action == Action.REMOVE_WORKER) {
            workerRegistry.deregister(currentWork);
            WorkerConfigurator.clearTaskQueue();

        } else if (action == Action.ADD_WORKER) {
            String group = action.parse(param.getData());
            if (!currentWork.getGroup().equals(group)) {
                throw new UnsupportedOperationException(
                    "Add worker failed, group expect '" + group + "', but actual '" + currentWork.getGroup() + "'"
                );
            }
            workerRegistry.register(currentWork);

        } else {
            throw new UnsupportedOperationException("Unsupported modify worker config action: " + param.getAction());
        }
    }

}

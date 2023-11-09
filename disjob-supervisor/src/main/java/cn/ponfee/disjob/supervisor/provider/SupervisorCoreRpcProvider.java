/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.provider;

import cn.ponfee.disjob.common.spring.RpcController;
import cn.ponfee.disjob.core.base.SupervisorCoreRpcService;
import cn.ponfee.disjob.core.enums.Operations;
import cn.ponfee.disjob.core.handle.execution.WorkflowPredecessorNode;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.core.param.StartTaskParam;
import cn.ponfee.disjob.core.param.TaskWorkerParam;
import cn.ponfee.disjob.core.param.TerminateTaskParam;
import cn.ponfee.disjob.supervisor.service.DistributedJobManager;
import cn.ponfee.disjob.supervisor.service.DistributedJobQuerier;

import java.util.List;

/**
 * Supervisor core rpc service provider.
 *
 * @author Ponfee
 */
public class SupervisorCoreRpcProvider implements SupervisorCoreRpcService, RpcController {

    private final DistributedJobManager jobManager;
    private final DistributedJobQuerier jobQuerier;

    public SupervisorCoreRpcProvider(DistributedJobManager jobManager,
                                     DistributedJobQuerier jobQuerier) {
        this.jobManager = jobManager;
        this.jobQuerier = jobQuerier;
    }

    @Override
    public SchedTask getTask(long taskId) {
        return jobQuerier.getTask(taskId);
    }

    @Override
    public boolean startTask(StartTaskParam param) {
        return jobManager.startTask(param);
    }

    @Override
    public void updateTaskWorker(List<TaskWorkerParam> params) {
        jobManager.updateTaskWorker(params);
    }

    @Override
    public List<WorkflowPredecessorNode> findWorkflowPredecessorNodes(long wnstanceId, long instanceId) {
        return jobQuerier.findWorkflowPredecessorNodes(wnstanceId, instanceId);
    }

    @Override
    public boolean terminateTask(TerminateTaskParam param) {
        return jobManager.terminateTask(param);
    }

    @Override
    public boolean pauseInstance(long instanceId) {
        return jobManager.pauseInstance(instanceId);
    }

    @Override
    public boolean cancelInstance(long instanceId, Operations ops) {
        return jobManager.cancelInstance(instanceId, ops);
    }

    @Override
    public void savepoint(long taskId, String executeSnapshot) {
        jobManager.savepoint(taskId, executeSnapshot);
    }

}

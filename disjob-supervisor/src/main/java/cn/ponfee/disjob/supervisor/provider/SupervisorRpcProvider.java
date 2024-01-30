/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.provider;

import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.spring.RpcController;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.SupervisorMetrics;
import cn.ponfee.disjob.core.base.SupervisorRpcService;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.enums.Operation;
import cn.ponfee.disjob.core.handle.execution.WorkflowPredecessorNode;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.core.param.supervisor.EventParam;
import cn.ponfee.disjob.core.param.supervisor.StartTaskParam;
import cn.ponfee.disjob.core.param.supervisor.TerminateTaskParam;
import cn.ponfee.disjob.core.param.supervisor.UpdateTaskWorkerParam;
import cn.ponfee.disjob.supervisor.application.EventSubscribeService;
import cn.ponfee.disjob.supervisor.auth.SupervisorAuthentication;
import cn.ponfee.disjob.supervisor.component.DistributedJobManager;
import cn.ponfee.disjob.supervisor.component.DistributedJobQuerier;

import java.util.List;

/**
 * Supervisor rpc provider.
 *
 * @author Ponfee
 */
@SupervisorAuthentication(SupervisorAuthentication.Subject.WORKER)
public class SupervisorRpcProvider implements SupervisorRpcService, RpcController {

    private final DistributedJobManager jobManager;
    private final DistributedJobQuerier jobQuerier;
    private final EventSubscribeService eventSubscribeService;

    public SupervisorRpcProvider(DistributedJobManager jobManager,
                                 DistributedJobQuerier jobQuerier,
                                 EventSubscribeService eventSubscribeService) {
        this.jobManager = jobManager;
        this.jobQuerier = jobQuerier;
        this.eventSubscribeService = eventSubscribeService;
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
    public void updateTaskWorker(List<UpdateTaskWorkerParam> list) {
        jobManager.updateTaskWorker(list);
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
    public boolean cancelInstance(long instanceId, Operation operation) {
        return jobManager.cancelInstance(instanceId, operation);
    }

    @SupervisorAuthentication(SupervisorAuthentication.Subject.ANON)
    @Override
    public SupervisorMetrics metrics() {
        SupervisorMetrics metrics = new SupervisorMetrics();
        metrics.setStartupAt(Dates.toDate(Supervisor.current().getStartupAt()));
        metrics.setAlsoWorker(Worker.current() != null);
        return metrics;
    }

    @SupervisorAuthentication(SupervisorAuthentication.Subject.ANON)
    @Override
    public void publish(EventParam param) {
        eventSubscribeService.subscribe(param);
    }

    @Override
    public void savepoint(long taskId, String executeSnapshot) {
        jobManager.savepoint(taskId, executeSnapshot);
    }

}

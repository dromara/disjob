/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.base;

import cn.ponfee.disjob.common.spring.RpcController;
import cn.ponfee.disjob.core.base.SupervisorService;
import cn.ponfee.disjob.core.enums.Operations;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.core.param.StartTaskParam;
import cn.ponfee.disjob.core.param.TaskWorkerParam;
import cn.ponfee.disjob.core.param.TerminateTaskParam;
import cn.ponfee.disjob.supervisor.manager.DistributedJobManager;

import java.util.List;

/**
 * Supervisor service provider.
 *
 * @author Ponfee
 */
public class SupervisorServiceProvider implements SupervisorService, RpcController {

    private final DistributedJobManager distributedJobManager;

    public SupervisorServiceProvider(DistributedJobManager distributedJobManager) {
        this.distributedJobManager = distributedJobManager;
    }

    @Override
    public SchedTask getTask(long taskId) {
        return distributedJobManager.getTask(taskId);
    }

    @Override
    public boolean startTask(StartTaskParam param) {
        return distributedJobManager.startTask(param);
    }

    @Override
    public void updateTaskWorker(List<TaskWorkerParam> params) {
        distributedJobManager.updateTaskWorker(params);
    }

    @Override
    public boolean terminateTask(TerminateTaskParam param) {
        return distributedJobManager.terminateTask(param);
    }

    @Override
    public boolean pauseInstance(long instanceId, Long wnstanceId) {
        return distributedJobManager.pauseInstance(instanceId, wnstanceId);
    }

    @Override
    public boolean cancelInstance(long instanceId, Long wnstanceId, Operations ops) {
        return distributedJobManager.cancelInstance(instanceId, wnstanceId, ops);
    }

    @Override
    public boolean checkpoint(long taskId, String executeSnapshot) {
        return distributedJobManager.checkpoint(taskId, executeSnapshot);
    }

}

/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.param;

import cn.ponfee.scheduler.common.graph.DAGNode;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.enums.JobType;
import cn.ponfee.scheduler.core.enums.Operations;
import cn.ponfee.scheduler.core.enums.RouteStrategy;
import cn.ponfee.scheduler.core.model.InstanceAttach;
import cn.ponfee.scheduler.core.model.SchedInstance;
import cn.ponfee.scheduler.core.model.SchedJob;
import org.springframework.util.Assert;

/**
 * Execute task param builder
 *
 * @author Ponfee
 */
public class ExecuteTaskParamBuilder {

    private final SchedInstance instance;
    private final SchedJob job;

    ExecuteTaskParamBuilder(SchedInstance instance, SchedJob schedJob) {
        this.instance = instance;
        this.job = schedJob;
    }

    public ExecuteTaskParam build(Operations ops, long taskId, long triggerTime, Worker worker) {
        String jobHandler;
        if (instance.getWorkflowInstanceId() != null) {
            Assert.hasText(instance.getAttach(), () -> "Workflow node instance attach cannot be null: " + instance.getInstanceId());
            InstanceAttach attach = Jsons.fromJson(instance.getAttach(), InstanceAttach.class);
            jobHandler = DAGNode.fromString(attach.getCurNode()).getName();
        } else {
            jobHandler = job.getJobHandler();
        }

        ExecuteTaskParam param = new ExecuteTaskParam(
            ops,
            taskId,
            instance.getInstanceId(),
            instance.getWorkflowInstanceId(),
            triggerTime,
            job.getJobId(),
            JobType.of(job.getJobType()),
            RouteStrategy.of(job.getRouteStrategy()),
            job.getExecuteTimeout(),
            jobHandler
        );
        param.setWorker(worker);

        return param;
    }

}

/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.param;

import cn.ponfee.disjob.common.dag.DAGNode;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.Operations;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.core.model.InstanceAttach;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedJob;
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
        if (instance.getWnstanceId() != null) {
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
            instance.getWnstanceId(),
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

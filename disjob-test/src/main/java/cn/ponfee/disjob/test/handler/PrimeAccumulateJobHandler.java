/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.test.handler;

import cn.ponfee.disjob.common.model.Result;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.handle.JobHandler;
import cn.ponfee.disjob.core.handle.Savepoint;
import cn.ponfee.disjob.core.handle.execution.AbstractExecutionTask;
import cn.ponfee.disjob.core.handle.execution.ExecutingTask;

/**
 * 质数计数后的累加器
 *
 * @author Ponfee
 */
public class PrimeAccumulateJobHandler extends JobHandler<Void> {

    @Override
    public Result<Void> execute(ExecutingTask executingTask, Savepoint savepoint) throws Exception {
        long sum = executingTask.getWorkflowPredecessorNodes()
            .stream()
            .flatMap(e -> e.getExecutedTasks().stream())
            .map(AbstractExecutionTask::getExecuteSnapshot)
            .map(e -> Jsons.fromJson(e, PrimeCountJobHandler.ExecuteSnapshot.class))
            .mapToLong(PrimeCountJobHandler.ExecuteSnapshot::getCount)
            .sum();
        savepoint.save(executingTask.getTaskId(), Long.toString(sum));

        return Result.success();
    }

}

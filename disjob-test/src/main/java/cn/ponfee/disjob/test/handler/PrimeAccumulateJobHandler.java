/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.test.handler;

import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.enums.RunState;
import cn.ponfee.disjob.core.handle.ExecuteResult;
import cn.ponfee.disjob.core.handle.JobHandler;
import cn.ponfee.disjob.core.handle.Savepoint;
import cn.ponfee.disjob.core.handle.execution.AbstractExecutionTask;
import cn.ponfee.disjob.core.handle.execution.ExecutingTask;
import org.springframework.util.Assert;

/**
 * 质数计数后的累加器
 *
 * @author Ponfee
 */
public class PrimeAccumulateJobHandler extends JobHandler {

    @Override
    public ExecuteResult execute(ExecutingTask executingTask, Savepoint savepoint) throws Exception {
        long sum = executingTask.getWorkflowPredecessorNodes()
            .stream()
            .peek(e -> Assert.state(RunState.FINISHED.equals(e.getRunState()), "Previous instance unfinished: " + e.getInstanceId()))
            .flatMap(e -> e.getExecutedTasks().stream())
            .map(AbstractExecutionTask::getExecuteSnapshot)
            .map(e -> Jsons.fromJson(e, PrimeCountJobHandler.ExecuteSnapshot.class))
            .mapToLong(PrimeCountJobHandler.ExecuteSnapshot::getCount)
            .sum();
        savepoint.save(Long.toString(sum));

        return ExecuteResult.success();
    }

}

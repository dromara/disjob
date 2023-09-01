/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.handle.execution;

import cn.ponfee.disjob.core.enums.ExecuteState;
import cn.ponfee.disjob.core.model.SchedTask;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Execution task converter test
 *
 * @author Ponfee
 */
public class ExecutionTaskConverterTest {

    @Test
    public void test1() {
        SchedTask task = new SchedTask();
        task.setExecuteState(ExecuteState.EXECUTING.value());

        ExecutedTask executedTask = ExecutionTaskConverter.INSTANCE.toExecutedTask(task);
        Assertions.assertEquals(executedTask.getExecuteState(), ExecuteState.EXECUTING);
    }
}

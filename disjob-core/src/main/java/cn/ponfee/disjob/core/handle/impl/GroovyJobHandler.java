/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.handle.impl;

import cn.ponfee.disjob.common.model.Result;
import cn.ponfee.disjob.common.util.GroovyUtils;
import cn.ponfee.disjob.core.handle.JobHandler;
import cn.ponfee.disjob.core.handle.Savepoint;
import cn.ponfee.disjob.core.handle.execution.ExecutingTask;
import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Objects;

/**
 *
 * The job handler for executes groovy script.
 * <p>
 *
 * <pre>job_params example: {@code
 *  import java.util.*
 *  def uuid = UUID.randomUUID().toString()
 *  savepoint.save(executingTask.taskId, uuid)
 *  return "execute at: " + new Date() + ", " + jobHandler.toString()
 * }</pre>
 *
 * @author Ponfee
 */
public class GroovyJobHandler extends JobHandler<String> {

    @Override
    public Result<String> execute(ExecutingTask executingTask, Savepoint savepoint) throws Exception {
        String scriptText = executingTask.getTaskParam();
        Map<String, Object> params = ImmutableMap.of(
            "jobHandler", this,
            "executingTask", executingTask,
            "savepoint", savepoint
        );

        Object result = GroovyUtils.Evaluator.SCRIPT.eval(scriptText, params);
        return Result.success(Objects.toString(result, null));
    }

}

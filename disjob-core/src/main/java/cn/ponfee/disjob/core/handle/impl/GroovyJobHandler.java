/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.core.handle.impl;

import cn.ponfee.disjob.common.util.GroovyUtils;
import cn.ponfee.disjob.core.handle.ExecuteResult;
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
 * <pre>job_param example: {@code
 *  import java.util.*
 *  def uuid = UUID.randomUUID().toString()
 *  savepoint.save(new Date().toString() + ": " + uuid)
 *  return "taskId: " + executingTask.getTaskId() + ", execute at: " + new Date() + ", " + jobHandler.toString()
 * }</pre>
 *
 * @author Ponfee
 */
public class GroovyJobHandler extends JobHandler {

    public static final String JOB_HANDLER = "jobHandler";
    public static final String EXECUTING_TASK = "executingTask";
    public static final String SAVEPOINT = "savepoint";

    @Override
    public ExecuteResult execute(ExecutingTask executingTask, Savepoint savepoint) throws Exception {
        String scriptText = executingTask.getTaskParam();
        Map<String, Object> params = ImmutableMap.of(
            JOB_HANDLER, this,
            EXECUTING_TASK, executingTask,
            SAVEPOINT, savepoint
        );

        Object result = GroovyUtils.Evaluator.SCRIPT.eval(scriptText, params);
        if (result instanceof ExecuteResult) {
            return (ExecuteResult) result;
        } else {
            return ExecuteResult.success(Objects.toString(result, null));
        }
    }

}

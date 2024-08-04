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

package cn.ponfee.disjob.worker.executor.impl;

import cn.ponfee.disjob.common.util.GroovyUtils;
import cn.ponfee.disjob.worker.executor.ExecutionResult;
import cn.ponfee.disjob.worker.executor.ExecutionTask;
import cn.ponfee.disjob.worker.executor.JobExecutor;
import cn.ponfee.disjob.worker.executor.Savepoint;
import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Objects;

/**
 *
 * The job executor for execute groovy script.
 * <p>
 *
 * <pre>job_param example: {@code
 *  import java.util.*
 *  def uuid = UUID.randomUUID().toString()
 *  savepoint.save(new Date().toString() + ": " + uuid)
 *  return "taskId=" + executionTask.getTaskId() + ", executeAt=" + new Date() + ", jobExecutor=" + jobExecutor.toString();
 * }</pre>
 *
 * @author Ponfee
 */
public class GroovyJobExecutor extends JobExecutor {

    public static final String JOB_EXECUTOR = "jobExecutor";
    public static final String EXECUTION_TASK = "executionTask";
    public static final String SAVEPOINT = "savepoint";

    @Override
    public ExecutionResult execute(ExecutionTask task, Savepoint savepoint) throws Exception {
        String scriptText = task.getTaskParam();
        Map<String, Object> params = ImmutableMap.of(
            JOB_EXECUTOR, this,
            EXECUTION_TASK, task,
            SAVEPOINT, savepoint
        );

        Object result = GroovyUtils.Evaluator.SCRIPT.eval(scriptText, params);
        if (result instanceof ExecutionResult) {
            return (ExecutionResult) result;
        } else {
            return ExecutionResult.success(Objects.toString(result, null));
        }
    }

}

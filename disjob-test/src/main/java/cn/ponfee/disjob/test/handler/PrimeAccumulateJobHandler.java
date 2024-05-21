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

package cn.ponfee.disjob.test.handler;

import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.enums.RunState;
import cn.ponfee.disjob.core.model.AbstractExecutionTask;
import cn.ponfee.disjob.worker.handle.ExecuteResult;
import cn.ponfee.disjob.worker.handle.ExecuteTask;
import cn.ponfee.disjob.worker.handle.JobHandler;
import cn.ponfee.disjob.worker.handle.Savepoint;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;

/**
 * 质数计数后的累加器
 *
 * @author Ponfee
 */
public class PrimeAccumulateJobHandler extends JobHandler {

    @Override
    public List<String> split(String jobParamString) {
        return Collections.singletonList(null);
    }

    @Override
    public ExecuteResult execute(ExecuteTask task, Savepoint savepoint) throws Exception {
        long sum = task.getWorkflowPredecessorNodes()
            .stream()
            .peek(e -> Assert.state(RunState.FINISHED == e.getRunState(), "Previous instance unfinished: " + e.getInstanceId()))
            .flatMap(e -> e.getExecutedTasks().stream())
            .map(AbstractExecutionTask::getExecuteSnapshot)
            .map(e -> Jsons.fromJson(e, PrimeCountJobHandler.ExecuteSnapshot.class))
            .mapToLong(PrimeCountJobHandler.ExecuteSnapshot::getCount)
            .sum();
        savepoint.save(Long.toString(sum));

        return ExecuteResult.success();
    }

}

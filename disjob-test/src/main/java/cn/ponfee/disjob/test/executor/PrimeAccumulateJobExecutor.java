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

package cn.ponfee.disjob.test.executor;

import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.dag.PredecessorTask;
import cn.ponfee.disjob.worker.executor.*;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 质数计数后的累加器
 *
 * @author Ponfee
 */
public class PrimeAccumulateJobExecutor extends BasicJobExecutor {

    private static final TypeReference<List<Param>> TYPE = new TypeReference<List<Param>>() {};

    @Override
    public List<String> split(BasicSplitParam splitParam) {
        List<Param> list = splitParam.getPredecessorInstances()
            .stream()
            .flatMap(e -> e.getTasks().stream())
            .map(Param::of)
            .collect(Collectors.toList());
        return Collections.singletonList(Jsons.toJson(list));
    }

    @Override
    public ExecutionResult execute(ExecutionTask task, Savepoint savepoint) throws Exception {
        long sum = Jsons.fromJson(task.getTaskParam(), TYPE).stream().mapToLong(Param::getPrimeCount).sum();
        savepoint.save(Long.toString(sum));
        return ExecutionResult.success();
    }

    @Getter
    @Setter
    public static class Param implements java.io.Serializable {
        private static final long serialVersionUID = 5822170830027680636L;
        private long taskId;
        private long primeCount;

        public static Param of(PredecessorTask task) {
            Param param = new Param();
            param.setTaskId(task.getTaskId());
            param.setPrimeCount(Jsons.fromJson(task.getExecuteSnapshot(), PrimeCountJobExecutor.ExecuteSnapshot.class).getCount());
            return param;
        }
    }

}

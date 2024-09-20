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

package cn.ponfee.disjob.test.util;

import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.test.executor.PrimeCountJobExecutor;
import cn.ponfee.disjob.worker.executor.ExecutionTask;
import cn.ponfee.disjob.worker.executor.Savepoint;
import cn.ponfee.disjob.worker.executor.SplitParam;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author Ponfee
 */
public class PrimeCountJobExecutorTest {

    @Test
    public void test() throws Exception {
        PrimeCountJobExecutor.JobParam jobParam = new PrimeCountJobExecutor.JobParam();
        jobParam.setM(3);
        jobParam.setN(9321);
        jobParam.setBlockSize(5613L);
        jobParam.setParallel(3);
        String json = Jsons.toJson(jobParam);
        System.out.println("jobParam: " + json);

        SchedJob job = new SchedJob();
        job.setJobParam(json);
        System.out.println(json);

        PrimeCountJobExecutor jobExecutor = new PrimeCountJobExecutor();
        SplitParam splitParam = new SplitParam();
        splitParam.setJobParam(job.getJobParam());
        List<String> taskParams = jobExecutor.split(splitParam);
        System.out.println(Jsons.toJson(taskParams));

        for (String taskParam : taskParams) {
            ExecutionTask task = new ExecutionTask();
            task.setTaskId(System.nanoTime());
            task.setTaskParam(taskParam);
            jobExecutor.execute(task, Savepoint.NOOP);
            System.out.println("-------------------");
        }
    }

}

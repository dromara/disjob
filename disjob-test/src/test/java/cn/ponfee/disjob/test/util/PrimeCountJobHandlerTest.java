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
import cn.ponfee.disjob.test.handler.PrimeCountJobHandler;
import cn.ponfee.disjob.worker.handle.ExecutingTask;
import cn.ponfee.disjob.worker.handle.Savepoint;
import cn.ponfee.disjob.worker.handle.SplitTask;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author Ponfee
 */
public class PrimeCountJobHandlerTest {

    @Test
    public void test() throws Exception {
        PrimeCountJobHandler.JobParam jobParam = new PrimeCountJobHandler.JobParam();
        jobParam.setM(3);
        jobParam.setN(9321);
        jobParam.setBlockSize(5613L);
        jobParam.setParallel(3);
        String json = Jsons.toJson(jobParam);
        System.out.println("jobParam: " + json);

        SchedJob schedJob = new SchedJob();
        schedJob.setJobParam(json);
        System.out.println(json);

        PrimeCountJobHandler jobHandler = new PrimeCountJobHandler();
        List<SplitTask> split = jobHandler.split(schedJob.getJobParam());
        System.out.println(Jsons.toJson(split));

        for (SplitTask splitTask : split) {
            ExecutingTask executingTask = new ExecutingTask();
            executingTask.setTaskId(System.nanoTime());
            executingTask.setTaskParam(splitTask.getTaskParam());
            jobHandler.execute(executingTask, Savepoint.DISCARD);
            System.out.println("-------------------");
        }
    }

}

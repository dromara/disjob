/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.test.util;

import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.handle.Checkpoint;
import cn.ponfee.disjob.core.handle.SplitTask;
import cn.ponfee.disjob.core.handle.execution.ExecutingTask;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.test.handler.PrimeCountJobHandler;
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
            jobHandler.execute(executingTask, Checkpoint.DISCARD);
            System.out.println("-------------------");
        }
    }

}

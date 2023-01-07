/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.samples;

import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.handle.Checkpoint;
import cn.ponfee.scheduler.core.handle.SplitTask;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.samples.common.handler.PrimeCountJobHandler;
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
            SchedTask schedTask = new SchedTask();
            schedTask.setTaskId(System.nanoTime());
            schedTask.setTaskParam(splitTask.getTaskParam());
            jobHandler.task(schedTask);
            jobHandler.execute(Checkpoint.DISCARD);
            System.out.println("-------------------");
        }
    }

}

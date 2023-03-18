/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.test.job.param;

import cn.ponfee.scheduler.common.base.model.Result;
import cn.ponfee.scheduler.common.util.Jsons;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.enums.JobType;
import cn.ponfee.scheduler.core.enums.Operations;
import cn.ponfee.scheduler.core.handle.Checkpoint;
import cn.ponfee.scheduler.core.handle.TaskExecutor;
import cn.ponfee.scheduler.core.param.ExecuteTaskParam;
import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Ponfee
 */
public class ExecuteParamTest {

    @Test
    public void testFastjson() {
        ExecuteTaskParam param = new ExecuteTaskParam(Operations.TRIGGER, 1, 2, 3, JobType.NORMAL, 4, null);
        Assertions.assertEquals("{\"operation\":\"TRIGGER\",\"taskId\":1,\"instanceId\":2,\"jobId\":3,\"jobType\":\"NORMAL\",\"triggerTime\":4}", Jsons.toJson(param));
        Assertions.assertEquals("{\"instanceId\":2,\"jobId\":3,\"jobType\":\"NORMAL\",\"operation\":\"TRIGGER\",\"taskId\":1,\"triggerTime\":4}", JSON.toJSONString(param));

        Worker worker = new Worker("g", "i", "h", 8081);
        param.setWorker(worker);
        param.taskExecutor(new TaskExecutor() {
            @Override
            public Result execute(Checkpoint checkpoint) {
                return null;
            }
        });
        String json = param.toString();
        System.out.println(json);
        Assertions.assertEquals(json, "{\"operation\":\"TRIGGER\",\"taskId\":1,\"instanceId\":2,\"jobId\":3,\"jobType\":\"NORMAL\",\"triggerTime\":4,\"worker\":{\"host\":\"h\",\"port\":8081,\"group\":\"g\",\"workerId\":\"i\"}}");
        Assertions.assertEquals(json, JSON.parseObject(json, ExecuteTaskParam.class).toString());
    }

    @Test
    public void testJackson() {
        ExecuteTaskParam param = new ExecuteTaskParam(Operations.TRIGGER, 1, 2, 3, JobType.NORMAL, 4, null);
        Worker worker = new Worker("g", "i", "h", 8081);
        param.setWorker(worker);
        param.taskExecutor(new TaskExecutor() {
            @Override
            public Result execute(Checkpoint checkpoint) {
                return null;
            }
        });
        String json = param.toString();
        System.out.println(json);
        Assertions.assertEquals(json, "{\"operation\":\"TRIGGER\",\"taskId\":1,\"instanceId\":2,\"jobId\":3,\"jobType\":\"NORMAL\",\"triggerTime\":4,\"worker\":{\"host\":\"h\",\"port\":8081,\"group\":\"g\",\"workerId\":\"i\"}}");
        Assertions.assertEquals(json, Jsons.fromJson(json, ExecuteTaskParam.class).toString());
    }

}

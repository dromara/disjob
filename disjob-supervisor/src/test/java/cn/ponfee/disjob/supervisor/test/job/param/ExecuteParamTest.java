/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.test.job.param;

import cn.ponfee.disjob.common.model.Result;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.Operations;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.core.handle.Checkpoint;
import cn.ponfee.disjob.core.handle.TaskExecutor;
import cn.ponfee.disjob.core.param.ExecuteTaskParam;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/*
import com.alibaba.fastjson.JSON;
*/

/**
 *
 * @author Ponfee
 */
public class ExecuteParamTest {

    @Test
    public void testFastjson() {
        ExecuteTaskParam param = new ExecuteTaskParam(Operations.TRIGGER, 1, 2, 1L, 3, 5, JobType.NORMAL, RouteStrategy.ROUND_ROBIN, 5, "jobHandler");
        Assertions.assertEquals("{\"operation\":\"TRIGGER\",\"taskId\":1,\"instanceId\":2,\"wnstanceId\":1,\"triggerTime\":3,\"jobId\":5,\"jobType\":\"NORMAL\",\"routeStrategy\":\"ROUND_ROBIN\",\"executeTimeout\":5,\"jobHandler\":\"jobHandler\"}", Jsons.toJson(param));
        //Assertions.assertEquals("{\"executeTimeout\":5,\"instanceId\":2,\"jobHandler\":\"jobHandler\",\"jobId\":5,\"jobType\":\"NORMAL\",\"operation\":\"TRIGGER\",\"routeStrategy\":\"ROUND_ROBIN\",\"taskId\":1,\"triggerTime\":3,\"wnstanceId\":1}", JSON.toJSONString(param));

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
        Assertions.assertEquals(json, "{\"operation\":\"TRIGGER\",\"taskId\":1,\"instanceId\":2,\"wnstanceId\":1,\"triggerTime\":3,\"jobId\":5,\"jobType\":\"NORMAL\",\"routeStrategy\":\"ROUND_ROBIN\",\"executeTimeout\":5,\"jobHandler\":\"jobHandler\",\"worker\":{\"host\":\"h\",\"port\":8081,\"group\":\"g\",\"workerId\":\"i\"}}");
        //Assertions.assertEquals(json, JSON.parseObject(json, ExecuteTaskParam.class).toString());
    }

    @Test
    public void testJackson() {
        ExecuteTaskParam param = new ExecuteTaskParam(Operations.TRIGGER, 1, 2, 1L, 4, 5, JobType.NORMAL, RouteStrategy.ROUND_ROBIN, 5, "jobHandler");
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
        Assertions.assertEquals(json, "{\"operation\":\"TRIGGER\",\"taskId\":1,\"instanceId\":2,\"wnstanceId\":1,\"triggerTime\":4,\"jobId\":5,\"jobType\":\"NORMAL\",\"routeStrategy\":\"ROUND_ROBIN\",\"executeTimeout\":5,\"jobHandler\":\"jobHandler\",\"worker\":{\"host\":\"h\",\"port\":8081,\"group\":\"g\",\"workerId\":\"i\"}}");
        Assertions.assertEquals(json, Jsons.fromJson(json, ExecuteTaskParam.class).toString());
    }

}

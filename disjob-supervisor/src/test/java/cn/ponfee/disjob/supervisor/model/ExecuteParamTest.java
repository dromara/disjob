/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.model;

import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.Operations;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.core.handle.ExecuteResult;
import cn.ponfee.disjob.core.handle.Savepoint;
import cn.ponfee.disjob.core.handle.TaskExecutor;
import cn.ponfee.disjob.core.handle.execution.ExecutingTask;
import cn.ponfee.disjob.core.param.ExecuteTaskParam;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Ponfee
 */
public class ExecuteParamTest {

    @Test
    public void test1() {
        ExecuteTaskParam param = new ExecuteTaskParam(Operations.TRIGGER, 1, 2, 1L, 3, 5, JobType.GENERAL, RouteStrategy.ROUND_ROBIN, 5, "jobHandler");
        Assertions.assertEquals("{\"operation\":\"TRIGGER\",\"taskId\":1,\"instanceId\":2,\"wnstanceId\":1,\"triggerTime\":3,\"jobId\":5,\"jobType\":\"GENERAL\",\"routeStrategy\":\"ROUND_ROBIN\",\"executeTimeout\":5,\"jobHandler\":\"jobHandler\"}", Jsons.toJson(param));

        Worker worker = new Worker("g", "i", "h", 8081);
        param.setWorker(worker);
        param.taskExecutor(new TaskExecutor() {
            @Override
            public ExecuteResult execute(ExecutingTask executingTask, Savepoint savepoint) {
                return null;
            }
        });
        String json = param.toString();
        System.out.println(json);
        Assertions.assertEquals(json, "{\"operation\":\"TRIGGER\",\"taskId\":1,\"instanceId\":2,\"wnstanceId\":1,\"triggerTime\":3,\"jobId\":5,\"jobType\":\"GENERAL\",\"routeStrategy\":\"ROUND_ROBIN\",\"executeTimeout\":5,\"jobHandler\":\"jobHandler\",\"worker\":{\"host\":\"h\",\"port\":8081,\"group\":\"g\",\"workerId\":\"i\"}}");
    }

    @Test
    public void test2() {
        ExecuteTaskParam param = new ExecuteTaskParam(Operations.TRIGGER, 1, 2, 1L, 4, 5, JobType.GENERAL, RouteStrategy.ROUND_ROBIN, 5, "jobHandler");
        Worker worker = new Worker("g", "i", "h", 8081);
        param.setWorker(worker);
        param.taskExecutor(new TaskExecutor() {
            @Override
            public ExecuteResult execute(ExecutingTask executingTask, Savepoint savepoint) {
                return null;
            }
        });
        String json = param.toString();
        System.out.println(json);
        Assertions.assertEquals(json, "{\"operation\":\"TRIGGER\",\"taskId\":1,\"instanceId\":2,\"wnstanceId\":1,\"triggerTime\":4,\"jobId\":5,\"jobType\":\"GENERAL\",\"routeStrategy\":\"ROUND_ROBIN\",\"executeTimeout\":5,\"jobHandler\":\"jobHandler\",\"worker\":{\"host\":\"h\",\"port\":8081,\"group\":\"g\",\"workerId\":\"i\"}}");
        Assertions.assertEquals(json, Jsons.fromJson(json, ExecuteTaskParam.class).toString());
    }

}

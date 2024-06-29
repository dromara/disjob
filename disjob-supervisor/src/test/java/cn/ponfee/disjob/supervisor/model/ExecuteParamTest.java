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

package cn.ponfee.disjob.supervisor.model;

import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.Operation;
import cn.ponfee.disjob.core.enums.RedeployStrategy;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import cn.ponfee.disjob.supervisor.util.CommonTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Ponfee
 */
public class ExecuteParamTest {

    @Test
    public void test1() {
        ExecuteTaskParam param = CommonTest.createExecuteTaskParam(Operation.TRIGGER, 1, 2, 1L, 3, 5, JobType.GENERAL, RouteStrategy.ROUND_ROBIN, RedeployStrategy.RESTART, 5, "jobHandler", new Worker("default", "workerId", "host", 1));
        Assertions.assertEquals("{\"supervisorToken\":\"supervisor token\",\"operation\":\"TRIGGER\",\"taskId\":1,\"instanceId\":2,\"wnstanceId\":1,\"triggerTime\":3,\"jobId\":5,\"jobType\":\"GENERAL\",\"routeStrategy\":\"ROUND_ROBIN\",\"redeployStrategy\":\"RESTART\",\"executeTimeout\":5,\"jobHandler\":\"jobHandler\",\"worker\":\"default:workerId:host:1\"}", Jsons.toJson(param));

        Worker worker = new Worker("g", "i", "h", 8081);
        param.setWorker(worker);
        String json = param.toString();
        System.out.println(json);
        Assertions.assertEquals("{\"supervisorToken\":\"supervisor token\",\"operation\":\"TRIGGER\",\"taskId\":1,\"instanceId\":2,\"wnstanceId\":1,\"triggerTime\":3,\"jobId\":5,\"jobType\":\"GENERAL\",\"routeStrategy\":\"ROUND_ROBIN\",\"redeployStrategy\":\"RESTART\",\"executeTimeout\":5,\"jobHandler\":\"jobHandler\",\"worker\":\"g:i:h:8081\"}", json);
    }

    @Test
    public void test2() {
        ExecuteTaskParam param = CommonTest.createExecuteTaskParam(Operation.TRIGGER, 1, 2, 1L, 4, 5, JobType.GENERAL, RouteStrategy.ROUND_ROBIN, RedeployStrategy.RESTART, 5, "jobHandler", new Worker("default", "workerId", "host", 1));
        Worker worker = new Worker("g", "i", "h", 8081);
        param.setWorker(worker);
        String json = param.toString();
        System.out.println(json);
        Assertions.assertEquals("{\"supervisorToken\":\"supervisor token\",\"operation\":\"TRIGGER\",\"taskId\":1,\"instanceId\":2,\"wnstanceId\":1,\"triggerTime\":4,\"jobId\":5,\"jobType\":\"GENERAL\",\"routeStrategy\":\"ROUND_ROBIN\",\"redeployStrategy\":\"RESTART\",\"executeTimeout\":5,\"jobHandler\":\"jobHandler\",\"worker\":\"g:i:h:8081\"}", json);
        Assertions.assertEquals(json, Jsons.fromJson(json, ExecuteTaskParam.class).toString());
    }

}

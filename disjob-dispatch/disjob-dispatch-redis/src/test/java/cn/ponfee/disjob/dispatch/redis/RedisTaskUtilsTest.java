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

package cn.ponfee.disjob.dispatch.redis;

import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.Operation;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.core.enums.ShutdownStrategy;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

/**
 * RedisTaskUtilsTest Test
 *
 * @author Ponfee
 */
public class RedisTaskUtilsTest {

    @Test
    void testSerialize() {
        ExecuteTaskParam param1 = new ExecuteTaskParam();
        param1.setOperation(Operation.TRIGGER);
        param1.setTaskId(ThreadLocalRandom.current().nextLong());
        param1.setInstanceId(ThreadLocalRandom.current().nextLong());
        param1.setWnstanceId(1L);
        param1.setTriggerTime(ThreadLocalRandom.current().nextLong());
        param1.setJobId(ThreadLocalRandom.current().nextLong());
        param1.setJobType(JobType.GENERAL);
        param1.setRouteStrategy(RouteStrategy.ROUND_ROBIN);
        param1.setShutdownStrategy(ShutdownStrategy.RESUME);
        param1.setExecuteTimeout(1);
        param1.setSupervisorAuthenticationToken("supervisor token");
        param1.setWorker(new Worker("default", "workerId", "host", 1));
        param1.setJobExecutor("JobExecutor测试中文乱码。");
        System.out.println(param1);

        ExecuteTaskParam param2 = RedisTaskUtils.deserialize(RedisTaskUtils.serialize(param1));
        Assertions.assertNotSame(param1, param2);
        Assertions.assertEquals(param1.toString(), param2.toString());
        Assertions.assertEquals(param1.getSupervisorAuthenticationToken(), param2.getSupervisorAuthenticationToken());
        Assertions.assertEquals(param1.getWorker(), param2.getWorker());
        Assertions.assertEquals(param1.getJobExecutor(), param2.getJobExecutor());
    }

}

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

package cn.ponfee.disjob.dispatch.route;

import cn.ponfee.disjob.common.base.ConsistentHash;
import cn.ponfee.disjob.common.util.UuidUtils;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.Operation;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * ConsistentHashExecutionRouter test
 *
 * @author Ponfee
 */
public class ConsistentHashExecutionRouterTest {

    @Test
    public void testList() {
        List<String> oldWorkers = Arrays.asList("1", "2", "3", "4", "5");
        List<String> newWorkers = Arrays.asList("3", "4", "5", "6", "7");
        Assertions.assertEquals("1,2", oldWorkers.stream().filter(e -> !newWorkers.contains(e)).collect(Collectors.joining(",")));
        Assertions.assertEquals("6,7", newWorkers.stream().filter(e -> !oldWorkers.contains(e)).collect(Collectors.joining(",")));
    }

    @Test
    public void testRegisterRouter() {
        ExecutionRouterRegistrar.register(new RandomExecutionRouter(null));
        ExecutionRouterRegistrar.register(new ConsistentHashExecutionRouter(11, ConsistentHash.HashFunction.MD5));
    }

    @Test
    public void testRouter() {
        List<ExecuteTaskParam> tasks = Arrays.asList(
            createExecuteTaskParam(1L),
            createExecuteTaskParam(2L),
            createExecuteTaskParam(3L),
            createExecuteTaskParam(4L),
            createExecuteTaskParam(5L)
        );
        List<Worker> workers = Arrays.asList(
            createWorker("a"),
            createWorker("b"),
            createWorker("c")
        );

        tasks.forEach(e -> Assertions.assertNull(e.getWorker()));

        ConsistentHashExecutionRouter router = new ConsistentHashExecutionRouter();
        router.route(tasks, workers);

        tasks.forEach(e -> Assertions.assertNotNull(e.getWorker()));

        workers = Arrays.asList(
            createWorker("b"),
            createWorker("c"),
            createWorker("d")
        );
        router.route(tasks, workers);
    }

    @Test
    public void test() {
        String key = UuidUtils.uuid32();
        String[] array = new String[ThreadLocalRandom.current().nextInt(17) + 3];
        array[0] = key;
        for (int i = 1; i < array.length; i++) {
            array[i] = RandomStringUtils.randomAlphanumeric(ThreadLocalRandom.current().nextInt(5) + 2);
        }
        List<String> workers = Arrays.asList(array);
        Collections.shuffle(workers);

        ConsistentHash<String> consistentHash = new ConsistentHash<>(workers, 11);
        Assertions.assertEquals(key, consistentHash.routeNode("SHARD-" + key + "-NODE-0"));
        Assertions.assertEquals(key, consistentHash.routeNode("SHARD-" + key + "-NODE-1"));
        Assertions.assertEquals(key, consistentHash.routeNode("SHARD-" + key + "-NODE-2"));
        Assertions.assertEquals(key, consistentHash.routeNode("SHARD-" + key + "-NODE-3"));
        Assertions.assertEquals(key, consistentHash.routeNode("SHARD-" + key + "-NODE-9"));
        Assertions.assertEquals(key, consistentHash.routeNode("SHARD-" + key + "-NODE-10"));

        Assertions.assertEquals(11, consistentHash.getExistingReplicas(key));
        Assertions.assertEquals(0, consistentHash.getExistingReplicas(key.toUpperCase()));
        Assertions.assertEquals(0, consistentHash.getExistingReplicas(key + "1"));

        Assertions.assertEquals(0, consistentHash.getExistingReplicas("a"));
        consistentHash.removeNode("a");
        Assertions.assertEquals(0, consistentHash.getExistingReplicas("a"));

        Assertions.assertEquals(11, consistentHash.getExistingReplicas(key));
        consistentHash.removeNode(key);
        Assertions.assertEquals(0, consistentHash.getExistingReplicas(key));
    }

    private static ExecuteTaskParam createExecuteTaskParam(long taskId) {
        return createExecuteTaskParam(Operation.TRIGGER, taskId, 1L, 1L, 0, 1L, JobType.GENERAL, RouteStrategy.CONSISTENT_HASH, 0, "");
    }

    private static Worker createWorker(String workerId) {
        return new Worker("default", workerId, "127.0.0.1", 80);
    }

    public static ExecuteTaskParam createExecuteTaskParam(Operation operation,
                                                          long taskId,
                                                          long instanceId,
                                                          Long wnstanceId,
                                                          long triggerTime,
                                                          long jobId,
                                                          JobType jobType,
                                                          RouteStrategy routeStrategy,
                                                          int executeTimeout,
                                                          String jobExecutor) {
        ExecuteTaskParam param = new ExecuteTaskParam();
        param.setOperation(operation);
        param.setTaskId(taskId);
        param.setInstanceId(instanceId);
        param.setWnstanceId(wnstanceId);
        param.setTriggerTime(triggerTime);
        param.setJobId(jobId);
        param.setJobType(jobType);
        param.setRouteStrategy(routeStrategy);
        param.setExecuteTimeout(executeTimeout);
        param.setJobExecutor(jobExecutor);
        return param;
    }
}

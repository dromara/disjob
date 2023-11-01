/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.route;

import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.Operations;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.core.param.ExecuteTaskParam;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
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

    private static ExecuteTaskParam createExecuteTaskParam(long taskId) {
        return new ExecuteTaskParam(Operations.TRIGGER, taskId, 1L, 1L, 0, 1L, JobType.NORMAL, RouteStrategy.CONSISTENT_HASH, 0, "");
    }

    private static Worker createWorker(String workerId) {
        return new Worker("default", workerId, "127.0.0.1", 80);
    }

}

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

package cn.ponfee.disjob.test.executor;

import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.worker.executor.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Test broadcast job executor
 *
 * @author Ponfee
 */
public class TestBroadcastJobExecutor extends JobExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(TestBroadcastJobExecutor.class);

    @Override
    public List<String> split(SplitParam param) throws JobException {
        Assert.isTrue(param.isBroadcast(), "Must be broadcast.");
        return IntStream.range(0, param.getWorkerCount())
            .mapToObj(i -> param.getJobParam() + "-" + i)
            .collect(Collectors.toList());
    }

    @Override
    public void init(ExecutionTask task) {
        LOG.debug("Broadcast job init.");
    }

    @Override
    public ExecutionResult execute(ExecutionTask task, Savepoint savepoint) throws Exception {
        Thread.sleep(5000 + ThreadLocalRandom.current().nextLong(10000));
        LOG.info("Broadcast job execute done: {}, {}", task.getTaskId(), task.isBroadcast());
        savepoint.save(Dates.format(new Date()) + ": " + getClass().getSimpleName());
        return ExecutionResult.success();
    }

}

/*
 * Copyright 2022-2026 Ponfee (http://www.ponfee.cn/)
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
import cn.ponfee.disjob.worker.executor.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * No operation job executor, use in test job executor.
 *
 * @author Ponfee
 */
@Slf4j
public class NoopJobExecutor extends JobExecutor {

    public static volatile long major = 9997;
    public static volatile long minor = 19997;

    @Override
    public List<String> split(SplitParam param) {
        return IntStream.range(0, 1 + ThreadLocalRandom.current().nextInt(5))
            .mapToObj(i -> getClass().getSimpleName() + "-" + i)
            .collect(Collectors.toList());
    }

    @Override
    public void init(ExecutionTask task) {
        log.debug("Noop job init.");
    }

    @Override
    public ExecutionResult execute(ExecutionTask task, Savepoint savepoint) throws Exception {
        log.info("task execute start: {}", task.getTaskId());
        Thread.sleep(major + ThreadLocalRandom.current().nextLong(minor));
        log.info("task execute done: {}", task.getTaskId());
        savepoint.save(Dates.format(new Date()) + ": " + getClass().getSimpleName());
        return ExecutionResult.success();
    }

}

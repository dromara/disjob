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

package cn.ponfee.disjob.test.handler;

import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.worker.handle.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * No operation job handler, use in test job handler.
 *
 * @author Ponfee
 */
public class NoopJobHandler extends JobHandler {

    private static final Logger LOG = LoggerFactory.getLogger(NoopJobHandler.class);
    public static volatile long major = 9997;
    public static volatile long minor = 19997;

    @Override
    public List<SplitTask> split(String jobParamString) {
        return IntStream.range(0, 1 + ThreadLocalRandom.current().nextInt(5))
            .mapToObj(i -> getClass().getSimpleName() + "-" + i)
            .map(SplitTask::new)
            .collect(Collectors.toList());
    }

    @Override
    public void init(ExecutingTask executingTask) {
        LOG.debug("Noop job init.");
    }

    @Override
    public ExecuteResult execute(ExecutingTask executingTask, Savepoint savepoint) throws Exception {
        LOG.info("task execute start: {}", executingTask.getTaskId());
        Thread.sleep(major + ThreadLocalRandom.current().nextLong(minor));
        LOG.info("task execute done: {}", executingTask.getTaskId());
        savepoint.save(Dates.format(new Date()) + ": " + getClass().getSimpleName());
        return ExecuteResult.success();
    }

}

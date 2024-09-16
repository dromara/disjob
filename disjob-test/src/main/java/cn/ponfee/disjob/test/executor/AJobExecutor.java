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
import cn.ponfee.disjob.test.util.Constants;
import cn.ponfee.disjob.worker.executor.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Workflow
 *
 * @author Ponfee
 */
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Service("AJobExecutor")
public class AJobExecutor extends BasicJobExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(AJobExecutor.class);

    @Override
    public List<String> split(BasicSplitParam splitParam) {
        return IntStream.range(0, Constants.TASK_COUNT)
            .mapToObj(i -> getClass().getSimpleName() + "-" + i)
            .collect(Collectors.toList());
    }

    @Override
    public ExecutionResult execute(ExecutionTask task, Savepoint savepoint) throws Exception {
        Thread.sleep(ThreadLocalRandom.current().nextInt(5000) + 1000L);
        LOG.info("Execution finished.");
        savepoint.save(Dates.format(new Date()) + ": " + getClass().getSimpleName());
        return ExecutionResult.success();
    }

}

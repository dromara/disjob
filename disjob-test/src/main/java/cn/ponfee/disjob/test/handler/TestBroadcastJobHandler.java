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
import cn.ponfee.disjob.worker.handle.BroadcastJobHandler;
import cn.ponfee.disjob.worker.handle.ExecuteResult;
import cn.ponfee.disjob.worker.handle.ExecuteTask;
import cn.ponfee.disjob.worker.handle.Savepoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Test broadcast job handler
 *
 * @author Ponfee
 */
public class TestBroadcastJobHandler extends BroadcastJobHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TestBroadcastJobHandler.class);

    @Override
    public void init(ExecuteTask task) {
        LOG.debug("Broadcast job init.");
    }

    @Override
    public ExecuteResult execute(ExecuteTask task, Savepoint savepoint) throws Exception {
        Thread.sleep(5000 + ThreadLocalRandom.current().nextLong(10000));
        LOG.info("Broadcast job execute done: {}", task.getTaskId());
        savepoint.save(Dates.format(new Date()) + ": " + getClass().getSimpleName());
        return ExecuteResult.success();
    }

}

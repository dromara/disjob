/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.test.handler;

import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.model.Result;
import cn.ponfee.disjob.core.handle.BroadcastJobHandler;
import cn.ponfee.disjob.core.handle.Checkpoint;
import cn.ponfee.disjob.core.handle.execution.ExecutingTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Test broadcast job handler
 *
 * @author Ponfee
 */
public class TestBroadcastJobHandler extends BroadcastJobHandler<Void> {

    private static final Logger LOG = LoggerFactory.getLogger(TestBroadcastJobHandler.class);

    @Override
    public void init(ExecutingTask executingTask) {
        LOG.debug("Broadcast job init.");
    }

    @Override
    public Result<Void> execute(ExecutingTask executingTask, Checkpoint checkpoint) throws Exception {
        Thread.sleep(5000 + ThreadLocalRandom.current().nextLong(10000));
        LOG.info("Broadcast job execute done: {}", executingTask.getTaskId());
        checkpoint.checkpoint(executingTask.getTaskId(), Dates.format(new Date()) + ": " + getClass().getSimpleName());
        return Result.success();
    }

}

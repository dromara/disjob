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
import cn.ponfee.disjob.core.handle.Checkpoint;
import cn.ponfee.disjob.core.handle.JobHandler;
import cn.ponfee.disjob.core.handle.SplitTask;
import cn.ponfee.disjob.core.handle.execution.ExecutingTask;
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
public class NoopJobHandler extends JobHandler<Void> {

    private static final Logger LOG = LoggerFactory.getLogger(NoopJobHandler.class);
    public static volatile long major = 9997;
    public static volatile long minor = 19997;

    @Override
    public List<SplitTask> split(String jobParamString) {
        return IntStream.range(0, 9).mapToObj(Integer::toString).map(SplitTask::new).collect(Collectors.toList());
    }

    @Override
    public void init(ExecutingTask executingTask) {
        LOG.debug("Noop job init.");
    }

    @Override
    public Result<Void> execute(ExecutingTask executingTask, Checkpoint checkpoint) throws Exception {
        LOG.info("task execute start: {}", executingTask.getTaskId());
        Thread.sleep(major + ThreadLocalRandom.current().nextLong(minor));
        LOG.info("task execute done: {}", executingTask.getTaskId());
        checkpoint.checkpoint(executingTask.getTaskId(), Dates.format(new Date()) + ": " + getClass().getSimpleName());
        return Result.success();
    }

}

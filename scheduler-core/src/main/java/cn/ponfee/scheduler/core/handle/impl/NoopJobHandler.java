package cn.ponfee.scheduler.core.handle.impl;

import cn.ponfee.scheduler.common.base.model.Result;
import cn.ponfee.scheduler.core.handle.Checkpoint;
import cn.ponfee.scheduler.core.handle.JobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

/**
 * No operation job handler.
 *
 * @author Ponfee
 */
public class NoopJobHandler extends JobHandler<Void> {

    private static final Logger LOG = LoggerFactory.getLogger(NoopJobHandler.class);
    public static volatile long major = 9997;
    public static volatile long minor = 19997;

    @Override
    public void init() {
        LOG.debug("Noop job init.");
    }

    @Override
    public Result<Void> execute(Checkpoint checkpoint) throws Exception {
        LOG.info("task execute start: {}", task().getTaskId());
        Thread.sleep(major + ThreadLocalRandom.current().nextLong(minor));
        LOG.info("task execute done: {}", task().getTaskId());
        return Result.success();
    }

}

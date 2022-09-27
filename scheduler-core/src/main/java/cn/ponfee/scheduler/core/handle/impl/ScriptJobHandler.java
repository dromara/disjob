package cn.ponfee.scheduler.core.handle.impl;

import cn.ponfee.scheduler.common.base.model.Result;
import cn.ponfee.scheduler.core.handle.Checkpoint;
import cn.ponfee.scheduler.core.handle.JobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The job handler for execute scripts.
 *
 * @author Ponfee
 */
public class ScriptJobHandler extends JobHandler<String> {

    private static final Logger LOG = LoggerFactory.getLogger(ScriptJobHandler.class);

    @Override
    public Result<String> execute(Checkpoint checkpoint) {
        throw new RuntimeException("unimplemented.");
    }
}

/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.samples.common.handler;

import cn.ponfee.scheduler.common.base.model.Result;
import cn.ponfee.scheduler.core.handle.Checkpoint;
import cn.ponfee.scheduler.core.handle.JobHandler;
import cn.ponfee.scheduler.core.handle.SplitTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

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
@Service("CJobHandler")
public class CJobHandler extends JobHandler<Void> {

    private final static Logger LOG = LoggerFactory.getLogger(CJobHandler.class);

    @Override
    public List<SplitTask> split(String jobParamString) {
        return IntStream.range(0, 20).mapToObj(Integer::toString).map(SplitTask::new).collect(Collectors.toList());
    }

    @Override
    public Result<Void> execute(Checkpoint checkpoint) throws Exception {
        Thread.sleep(ThreadLocalRandom.current().nextInt(5000) + 1000);
        LOG.info(this.getClass().getSimpleName() + " execution finished.");
        return Result.SUCCESS;
    }

}

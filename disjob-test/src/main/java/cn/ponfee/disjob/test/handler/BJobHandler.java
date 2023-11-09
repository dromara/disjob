/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.test.handler;

import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.core.handle.ExecuteResult;
import cn.ponfee.disjob.core.handle.JobHandler;
import cn.ponfee.disjob.core.handle.Savepoint;
import cn.ponfee.disjob.core.handle.SplitTask;
import cn.ponfee.disjob.core.handle.execution.ExecutingTask;
import cn.ponfee.disjob.test.util.Constants;
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
@Service("BJobHandler")
public class BJobHandler extends JobHandler {

    private final static Logger LOG = LoggerFactory.getLogger(BJobHandler.class);

    @Override
    public List<SplitTask> split(String jobParamString) {
        return IntStream.range(0, Constants.TASK_COUNT)
            .mapToObj(i -> getClass().getSimpleName() + "-" + i)
            .map(SplitTask::new)
            .collect(Collectors.toList());
    }

    @Override
    public ExecuteResult execute(ExecutingTask executingTask, Savepoint savepoint) throws Exception {
        Thread.sleep(ThreadLocalRandom.current().nextInt(5000) + 1000);
        LOG.info(this.getClass().getSimpleName() + " execution finished.");
        savepoint.save(Dates.format(new Date()) + ": " + getClass().getSimpleName());
        return ExecuteResult.success();
    }

}

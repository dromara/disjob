/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.worker.rpc;

import cn.ponfee.scheduler.common.spring.MarkRpcController;
import cn.ponfee.scheduler.core.base.WorkerService;
import cn.ponfee.scheduler.core.exception.JobException;
import cn.ponfee.scheduler.core.handle.JobHandlerUtils;
import cn.ponfee.scheduler.core.handle.SplitTask;

import java.util.List;

/**
 * Worker service provided by remote.
 *
 * @author Ponfee
 */
public class WorkerRemote implements WorkerService, MarkRpcController {

    @Override
    public boolean verify(String jobHandler, String jobParam) {
        return JobHandlerUtils.verify(jobHandler, jobParam);
    }

    @Override
    public List<SplitTask> split(String jobHandler, String jobParam) throws JobException {
        return JobHandlerUtils.split(jobHandler, jobParam);
    }

}

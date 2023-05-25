/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.worker.rpc;

import cn.ponfee.disjob.common.spring.RpcController;
import cn.ponfee.disjob.core.base.WorkerService;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.handle.JobHandlerUtils;
import cn.ponfee.disjob.core.handle.SplitTask;
import cn.ponfee.disjob.core.param.JobHandlerParam;

import java.util.List;

/**
 * Worker service provider.
 *
 * @author Ponfee
 */
public class WorkerServiceProvider implements WorkerService, RpcController {

    @Override
    public void verify(JobHandlerParam param) throws JobException {
        JobHandlerUtils.verify(param);
    }

    @Override
    public List<SplitTask> split(JobHandlerParam param) throws JobException {
        return JobHandlerUtils.split(param);
    }

}

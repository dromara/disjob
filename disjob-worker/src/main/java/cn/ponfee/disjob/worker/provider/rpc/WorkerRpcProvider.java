/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.worker.provider.rpc;

import cn.ponfee.disjob.common.spring.RpcController;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.base.WorkerRpcService;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.handle.JobHandlerUtils;
import cn.ponfee.disjob.core.handle.SplitTask;
import cn.ponfee.disjob.core.param.worker.JobHandlerParam;

import java.util.List;

/**
 * Worker rpc service provider.
 *
 * @author Ponfee
 */
public class WorkerRpcProvider implements WorkerRpcService, RpcController {

    private final Worker.Current currentWork;

    public WorkerRpcProvider(Worker.Current currentWork) {
        this.currentWork = currentWork;
    }

    @Override
    public void verify(JobHandlerParam param) throws JobException {
        currentWork.authenticate(param);
        JobHandlerUtils.verify(param);
    }

    @Override
    public List<SplitTask> split(JobHandlerParam param) throws JobException {
        currentWork.authenticate(param);
        return JobHandlerUtils.split(param);
    }

    @Override
    public boolean isSupervisor() {
        return Supervisor.current() != null;
    }

}

package cn.ponfee.scheduler.worker.rpc;

import cn.ponfee.scheduler.common.spring.MarkRpcController;
import cn.ponfee.scheduler.core.base.WorkerLocal;
import cn.ponfee.scheduler.core.base.WorkerService;
import cn.ponfee.scheduler.core.exception.JobException;
import cn.ponfee.scheduler.core.handle.SplitTask;

import java.util.List;

/**
 * Worker service provided by remote.
 *
 * @author Ponfee
 */
public class WorkerRemote implements WorkerService, MarkRpcController {

    private final WorkerLocal delegate = new WorkerLocal();

    @Override
    public boolean verify(String jobHandler, String jobParam) {
        return delegate.verify(jobHandler, jobParam);
    }

    @Override
    public List<SplitTask> split(String jobHandler, String jobParam) throws JobException {
        return delegate.split(jobHandler, jobParam);
    }

}

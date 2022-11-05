package cn.ponfee.scheduler.core.base;

import cn.ponfee.scheduler.core.exception.JobException;
import cn.ponfee.scheduler.core.handle.JobHandlerUtils;
import cn.ponfee.scheduler.core.handle.SplitTask;

import java.util.List;

/**
 * Worker service provided by local.
 *
 * @author Ponfee
 */
public class WorkerLocal implements WorkerService {

    @Override
    public boolean verify(String jobHandler, String jobParam) {
        return JobHandlerUtils.verify(jobHandler, jobParam);
    }

    @Override
    public List<SplitTask> split(String jobHandler, String jobParam) throws JobException {
        return JobHandlerUtils.split(jobHandler, jobParam);
    }

}

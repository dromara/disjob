package cn.ponfee.scheduler.core.base;

import cn.ponfee.scheduler.common.base.model.Result;
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
    public Result<Boolean> verify(String jobHandler, String jobParam) {
        return Result.success(JobHandlerUtils.verify(jobHandler, jobParam));
    }

    @Override
    public Result<List<SplitTask>> split(String jobHandler, String jobParam) {
        try {
            return Result.success(JobHandlerUtils.split(jobHandler, jobParam));
        } catch (JobException e) {
            return Result.failure(e.getCode(), e.getMessage());
        }
    }

}

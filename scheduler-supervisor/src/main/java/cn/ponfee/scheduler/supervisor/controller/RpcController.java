package cn.ponfee.scheduler.supervisor.controller;

import cn.ponfee.scheduler.common.base.model.Result;
import cn.ponfee.scheduler.common.spring.LocalizedMethodArgumentResolver;
import cn.ponfee.scheduler.common.spring.LocalizedMethodArguments;
import cn.ponfee.scheduler.core.enums.ExecuteState;
import cn.ponfee.scheduler.core.enums.Operations;
import cn.ponfee.scheduler.core.exception.JobException;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.supervisor.manager.JobManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Provides to internal invoke, for the worker communication.
 *
 * @author Ponfee
 * @see LocalizedMethodArgumentResolver
 */
@RestController
@RequestMapping("rpc")
@LocalizedMethodArguments
public class RpcController {

    private final JobManager jobManager;

    public RpcController(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @PostMapping("job/get")
    public Result<SchedJob> getJob(long jobId) {
        return Result.success(jobManager.getJob(jobId));
    }

    @PostMapping("task/get")
    public Result<SchedTask> getTask(long taskId) {
        return Result.success(jobManager.getTask(taskId));
    }

    @PostMapping("task/start")
    public Result<Boolean> startTask(ExecuteParam param) {
        return Result.success(jobManager.startTask(param));
    }

    @PostMapping("task/checkpoint")
    public Result<Boolean> checkpointTask(long taskId, String executeSnapshot) {
        return Result.success(jobManager.checkpointTask(taskId, executeSnapshot));
    }

    @PostMapping("task_error_msg/update")
    public Result<Boolean> updateTaskErrorMsg(long taskId, String errorMsg) {
        return Result.success(jobManager.updateTaskErrorMsg(taskId, errorMsg));
    }

    @PostMapping("track/pause")
    public Result<Boolean> pauseTrack(long trackId) throws JobException {
        return Result.success(jobManager.pauseTrack(trackId));
    }

    @PostMapping("track/cancel")
    public Result<Boolean> cancelTrack(long trackId, Operations operation) throws JobException {
        return Result.success(jobManager.cancelTrack(trackId, operation));
    }

    @PostMapping("executing_task/pause")
    public Result<Boolean> pauseExecutingTask(ExecuteParam param, String errorMsg) {
        return Result.success(jobManager.pauseExecutingTask(param, errorMsg));
    }

    @PostMapping("executing_task/cancel")
    public Result<Boolean> cancelExecutingTask(ExecuteParam param, ExecuteState toState, String errorMsg) {
        return Result.success(jobManager.cancelExecutingTask(param, toState, errorMsg));
    }

    @PostMapping("executing_task/terminate")
    public Result<Boolean> terminateExecutingTask(ExecuteParam param, ExecuteState toState, String errorMsg) {
        return Result.success(jobManager.terminateExecutingTask(param, toState, errorMsg));
    }

}

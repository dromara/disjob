package cn.ponfee.scheduler.core.base;

import cn.ponfee.scheduler.core.enums.ExecuteState;
import cn.ponfee.scheduler.core.enums.Operations;
import cn.ponfee.scheduler.core.handle.Checkpoint;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Supervisor provides api, for the worker communication.
 *
 * @author Ponfee
 */
@RequestMapping("supervisor/rpc")
public interface SupervisorService extends Checkpoint {

    @PostMapping("job/get")
    SchedJob getJob(long jobId) throws Exception;

    @PostMapping("task/get")
    SchedTask getTask(long taskId) throws Exception;

    @PostMapping("task/start")
    boolean startTask(ExecuteParam param) throws Exception;

    @PostMapping("task_error_msg/update")
    boolean updateTaskErrorMsg(long taskId, String errorMsg) throws Exception;

    @PostMapping("track/pause")
    boolean pauseTrack(long trackId) throws Exception;

    @PostMapping("track/cancel")
    boolean cancelTrack(long trackId, Operations operation) throws Exception;

    @PostMapping("executing_task/pause")
    boolean pauseExecutingTask(ExecuteParam param, String errorMsg) throws Exception;

    @PostMapping("executing_task/cancel")
    boolean cancelExecutingTask(ExecuteParam param, ExecuteState toState, String errorMsg) throws Exception;

    @PostMapping("executing_task/terminate")
    boolean terminateExecutingTask(ExecuteParam param, ExecuteState toState, String errorMsg) throws Exception;

    // ---------------------------------------------------------------------------checkpoint

    @Override
    @PostMapping("task/checkpoint")
    boolean checkpoint(long taskId, String executeSnapshot) throws Exception;
}

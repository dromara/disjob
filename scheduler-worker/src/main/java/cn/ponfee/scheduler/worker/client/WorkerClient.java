package cn.ponfee.scheduler.worker.client;

import cn.ponfee.scheduler.core.enums.ExecuteState;
import cn.ponfee.scheduler.core.enums.Operations;
import cn.ponfee.scheduler.core.handle.Checkpoint;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.core.param.ExecuteParam;

/**
 * Worker client for provides sched task executing operations.
 *
 * @author Ponfee
 */
public interface WorkerClient extends Checkpoint {

    SchedJob getJob(long jobId) throws Exception;

    SchedTask getTask(long taskId) throws Exception;

    boolean startTask(ExecuteParam param) throws Exception;

    boolean updateTaskErrorMsg(long taskId, String errorMsg) throws Exception;

    boolean pauseTrack(long trackId) throws Exception;

    boolean cancelTrack(long trackId, Operations operations) throws Exception;

    boolean pauseExecutingTask(ExecuteParam param, String errorMsg) throws Exception;

    boolean cancelExecutingTask(ExecuteParam param, ExecuteState toState, String errorMsg) throws Exception;

    boolean terminateExecutingTask(ExecuteParam param, ExecuteState toState, String errorMsg) throws Exception;
}

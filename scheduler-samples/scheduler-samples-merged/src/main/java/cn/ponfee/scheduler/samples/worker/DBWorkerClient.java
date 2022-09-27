package cn.ponfee.scheduler.samples.worker;

import cn.ponfee.scheduler.core.enums.ExecuteState;
import cn.ponfee.scheduler.core.enums.Operations;
import cn.ponfee.scheduler.core.exception.JobException;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.supervisor.manager.JobManager;
import cn.ponfee.scheduler.worker.client.WorkerClient;

/**
 * Worker client based local direct-connect database
 *
 * @author Ponfee
 */
public class DBWorkerClient implements WorkerClient {

    private final JobManager jobManager;

    public DBWorkerClient(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @Override
    public SchedJob getJob(long jobId) {
        return jobManager.getJob(jobId);
    }

    @Override
    public SchedTask getTask(long taskId) {
        return jobManager.getTask(taskId);
    }

    @Override
    public boolean startTask(ExecuteParam param) {
        return jobManager.startTask(param);
    }

    @Override
    public boolean checkpoint(long taskId, String executeSnapshot) {
        return jobManager.checkpointTask(taskId, executeSnapshot);
    }

    @Override
    public boolean updateTaskErrorMsg(long taskId, String errorMsg) {
        return jobManager.updateTaskErrorMsg(taskId, errorMsg);
    }

    @Override
    public boolean pauseTrack(long trackId) throws JobException {
        return jobManager.pauseTrack(trackId);
    }

    @Override
    public boolean cancelTrack(long trackId, Operations operations) throws JobException {
        return jobManager.cancelTrack(trackId, operations);
    }

    @Override
    public boolean pauseExecutingTask(ExecuteParam param, String errorMsg) {
        return jobManager.pauseExecutingTask(param, errorMsg);
    }

    @Override
    public boolean cancelExecutingTask(ExecuteParam param, ExecuteState toState, String errorMsg) {
        return jobManager.cancelExecutingTask(param, toState, errorMsg);
    }

    @Override
    public boolean terminateExecutingTask(ExecuteParam param, ExecuteState toState, String errorMsg) {
        return jobManager.terminateExecutingTask(param, toState, errorMsg);
    }

}

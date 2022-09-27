package cn.ponfee.scheduler.dispatch;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.exception.JobException;
import cn.ponfee.scheduler.core.param.DispatchParam;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.registry.Discovery;

import javax.annotation.Nullable;
import java.util.List;

import static cn.ponfee.scheduler.core.base.JobCodeMsg.*;

/**
 * Supervisor job task dispatcher.
 *
 * @author Ponfee
 */
public abstract class TaskDispatcher implements AutoCloseable {

    private final Discovery<Worker> discoveryWorker;
    private final TimingWheel<ExecuteParam> timingWheel;

    public TaskDispatcher(Discovery<Worker> discoveryWorker,
                          @Nullable TimingWheel<ExecuteParam> timingWheel) {
        this.discoveryWorker = discoveryWorker;
        this.timingWheel = timingWheel;
    }

    /**
     * Dispatch the tasks to remote worker
     * 
     * @param executeParam the execution param
     * @return {@code true} if dispatched successful
     * @throws JobException if dispatch occurred exception.
     */
    protected abstract boolean dispatch(ExecuteParam executeParam) throws JobException;

    /**
     * Dispatch the tasks to assigned worker
     * <p>this method use in pause or cancel executing task
     *
     * @param executeParams the list of execution param
     * @throws JobException if dispatch occurred exception.
     */
    public final void dispatch(List<ExecuteParam> executeParams) throws JobException {
        Worker current = Worker.current();

        for (ExecuteParam param : executeParams) {
            boolean status = (timingWheel != null && param.getWorker().equals(current))
                           ? timingWheel.offer(param) // push to local worker
                           : dispatch(param);         // push to remote worker
            if (!status) {
                throw new JobException(DISPATCH_TASK_FAILED, "Dispatch task failed: " + executeParams);
            }
        }
    }

    /**
     * Assign worker and dispatch to worker.
     *
     * @param dispatchParam the task dispatch param.
     * @return list of dispatched task execution param
     * @throws JobException if dispatch occurred exception.
     */
    public final List<ExecuteParam> dispatch(DispatchParam dispatchParam) throws JobException {
        List<Worker> workers = discoveryWorker.getServers(dispatchParam.getGroup());
        Worker current = Worker.current();

        List<ExecuteParam> executableTasks = dispatchParam.toExecuteParams();
        for (ExecuteParam param : executableTasks) {
            Worker worker = dispatchParam.getRouteStrategy().route(param, workers);
            if (worker == null && current != null && current.matches(dispatchParam.getGroup())) {
                worker = current;
            }
            if (worker == null) {
                throw new JobException(WORKER_NOT_FOUND, "Not found group worker: " + dispatchParam.getGroup() );
            }
            if (!worker.matches(dispatchParam.getGroup())) {
                throw new JobException(GROUP_NOT_MATCH, "Unmatched group: " + dispatchParam.getGroup() + "!=" + worker.getGroup());
            }

            param.setWorker(worker);
        }

        dispatch(executableTasks);

        return executableTasks;
    }

    /**
     * Closed resources.
     */
    @Override
    public void close() {
        // No-op
    }

}

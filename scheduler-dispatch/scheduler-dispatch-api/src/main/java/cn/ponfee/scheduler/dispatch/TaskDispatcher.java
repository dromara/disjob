/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.dispatch;

import cn.ponfee.scheduler.common.base.TimingWheel;
import cn.ponfee.scheduler.common.concurrent.AsyncDelayedExecutor;
import cn.ponfee.scheduler.common.concurrent.DelayedData;
import cn.ponfee.scheduler.core.base.Worker;
import cn.ponfee.scheduler.core.enums.Operations;
import cn.ponfee.scheduler.core.enums.RouteStrategy;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.core.model.SchedTrack;
import cn.ponfee.scheduler.core.param.ExecuteParam;
import cn.ponfee.scheduler.registry.Discovery;
import com.google.common.math.IntMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Supervisor dispatching task to worker.
 *
 * @author Ponfee
 */
public abstract class TaskDispatcher implements AutoCloseable {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final Discovery<Worker> discoveryWorker;
    private final TimingWheel<ExecuteParam> timingWheel;

    private final int maxRetryTimes;
    private final AsyncDelayedExecutor<DispatchParam> asyncDelayedExecutor;

    public TaskDispatcher(Discovery<Worker> discoveryWorker,
                          @Nullable TimingWheel<ExecuteParam> timingWheel) {
        this(discoveryWorker, timingWheel, 5);
    }

    public TaskDispatcher(Discovery<Worker> discoveryWorker,
                          @Nullable TimingWheel<ExecuteParam> timingWheel,
                          int maxRetryTimes) {
        this.discoveryWorker = discoveryWorker;
        this.timingWheel = timingWheel;

        this.maxRetryTimes = maxRetryTimes;
        this.asyncDelayedExecutor = new AsyncDelayedExecutor<>(this::doDispatch);
    }

    /**
     * Dispatch the tasks to remote worker
     *
     * @param executeParam the execution param
     * @return {@code true} if dispatched successful
     * @throws Exception if dispatch occur error
     */
    protected abstract boolean dispatch(ExecuteParam executeParam) throws Exception;

    /**
     * Dispatch the task to specified worker, which the worker is executing this task
     * <p>this method use in pause or cancel executing task
     *
     * @param executeParams the list of execution param
     * @return {@code true} if the first dispatch successful
     */
    public final boolean dispatch(List<ExecuteParam> executeParams) {
        List<DispatchParam> list = executeParams.stream()
            .peek(e -> Assert.notNull(e.getWorker(), "Dispatching execute param worker cannot be null."))
            .peek(e -> Assert.isTrue(e.operation() != Operations.TRIGGER, "Dispatching execute param operation cannot be TRIGGER."))
            .map(e -> new DispatchParam(e, null, null))
            .collect(Collectors.toList());
        return doDispatch(list);
    }

    /**
     * Assign a worker and dispatch to the assigned worker.
     *
     * @param job   the job
     * @param track the track
     * @param tasks the list of task
     * @return {@code true} if the first dispatch successful
     */
    public final boolean dispatch(SchedJob job, SchedTrack track, List<SchedTask> tasks) {
        List<DispatchParam> dispatchParams = new ArrayList<>(tasks.size());
        for (SchedTask task : tasks) {
            ExecuteParam executeParam = new ExecuteParam(
                Operations.TRIGGER,
                task.getTaskId(),
                track.getTrackId(),
                job.getJobId(),
                track.getTriggerTime()
            );
            DispatchParam dispatchParam = new DispatchParam(
                executeParam,
                job.getJobGroup(),
                RouteStrategy.of(job.getRouteStrategy())
            );
            dispatchParams.add(dispatchParam);
        }

        return doDispatch(dispatchParams);
    }

    /**
     * Closed resources.
     */
    @Override
    public void close() {
        // No-op
    }

    private boolean doDispatch(DispatchParam dispatchParam) {
        return doDispatch(Collections.singletonList(dispatchParam));
    }

    private boolean doDispatch(List<DispatchParam> dispatchParams) {
        Worker current = Worker.current();
        boolean result = true;
        for (DispatchParam dispatchParam : dispatchParams) {
            assignWorker(dispatchParam);

            ExecuteParam executeParam = dispatchParam.executeParam();
            if (executeParam.getWorker() == null) {
                // not found worker, delay retry
                retry(dispatchParam);
                result = false;
                continue;
            }
            try {
                boolean status = (timingWheel != null && executeParam.getWorker().equals(current))
                               ? timingWheel.offer(executeParam) // push to local worker
                               : dispatch(executeParam);         // push to remote worker
                if (!status) {
                    // dispatch failed, delay retry
                    retry(dispatchParam);
                    log.error("Dispatch task failed: " + dispatchParam);
                    result = false;
                }
            } catch (Exception e) {
                // dispatch error, delay retry
                retry(dispatchParam);
                log.error("Dispatch task error: " + dispatchParam, e);
                result = false;
            }
        }

        return result;
    }

    private void assignWorker(DispatchParam dispatchParam) {
        ExecuteParam executeParam = dispatchParam.executeParam();
        if (executeParam.operation() != Operations.TRIGGER) {
            // if pause or cancel, cannot assign worker
            return;
        }

        List<Worker> workers = discoveryWorker.getDiscoveredServers(dispatchParam.group());
        Worker worker = dispatchParam.routeStrategy().route(executeParam, workers);
        executeParam.setWorker(worker);
    }

    private void retry(DispatchParam dispatchParam) {
        if (dispatchParam.retried() >= maxRetryTimes) {
            // discard
            log.warn("Dispatched task retried max times still failed: " + dispatchParam.executeParam());
            return;
        }

        dispatchParam.retrying();
        asyncDelayedExecutor.put(new DelayedData<>(dispatchParam, 1000 * IntMath.pow(dispatchParam.retried(), 2)));
    }

}

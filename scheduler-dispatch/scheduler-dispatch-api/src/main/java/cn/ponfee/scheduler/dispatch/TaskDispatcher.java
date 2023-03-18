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
import cn.ponfee.scheduler.core.enums.JobType;
import cn.ponfee.scheduler.core.enums.Operations;
import cn.ponfee.scheduler.core.enums.RouteStrategy;
import cn.ponfee.scheduler.core.model.SchedInstance;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.core.param.ExecuteTaskParam;
import cn.ponfee.scheduler.core.route.ExecutionRouter;
import cn.ponfee.scheduler.core.route.ExecutionRouterRegistrar;
import cn.ponfee.scheduler.registry.Discovery;
import com.google.common.math.IntMath;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Supervisor dispatching task to worker.
 *
 * @author Ponfee
 */
public abstract class TaskDispatcher implements AutoCloseable {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final Discovery<Worker> discoveryWorker;
    private final TimingWheel<ExecuteTaskParam> timingWheel;

    private final int maxRetryTimes;
    private final AsyncDelayedExecutor<DispatchParam> asyncDelayedExecutor;

    public TaskDispatcher(Discovery<Worker> discoveryWorker,
                          @Nullable TimingWheel<ExecuteTaskParam> timingWheel) {
        this(discoveryWorker, timingWheel, 3);
    }

    public TaskDispatcher(Discovery<Worker> discoveryWorker,
                          @Nullable TimingWheel<ExecuteTaskParam> timingWheel,
                          int maxRetryTimes) {
        this.discoveryWorker = discoveryWorker;
        this.timingWheel = timingWheel;

        this.maxRetryTimes = maxRetryTimes;
        this.asyncDelayedExecutor = new AsyncDelayedExecutor<>(3, e -> doDispatch(Collections.singletonList(e)));
    }

    /**
     * Dispatch the tasks to remote worker
     *
     * @param param the execution task param
     * @return {@code true} if dispatched successful
     * @throws Exception if dispatch occur error
     */
    protected abstract boolean dispatch(ExecuteTaskParam param) throws Exception;

    /**
     * Dispatch the task to specified worker, which the worker is executing this task
     * <p>this method is used to terminate(pause or cancel) the executing task
     *
     * @param param the list of execution task param
     * @return {@code true} if the first dispatch successful
     */
    public final boolean dispatch(List<ExecuteTaskParam> param) {
        if (CollectionUtils.isEmpty(param)) {
            return false;
        }
        List<DispatchParam> list = param.stream()
            .peek(e -> Assert.notNull(e.getWorker(), "Directional dispatching execute param worker cannot be null."))
            .peek(e -> Assert.isTrue(e.operation() != Operations.TRIGGER, "Directional dispatching execute param operation cannot be TRIGGER."))
            .map(e -> new DispatchParam(e, null, null))
            .collect(Collectors.toList());
        return doDispatch(list);
    }

    /**
     * Assign a worker and dispatch to the assigned worker.
     *
     * @param job      the job
     * @param instance the instance
     * @param tasks    the list of task
     * @return {@code true} if the first dispatch successful
     */
    public final boolean dispatch(SchedJob job, SchedInstance instance, List<SchedTask> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return false;
        }
        JobType jobType = JobType.of(job.getJobType());
        RouteStrategy routeStrategy = RouteStrategy.of(job.getRouteStrategy());

        List<DispatchParam> dispatchParams = new ArrayList<>(tasks.size());
        for (SchedTask task : tasks) {
            Worker worker = null;
            if (jobType == JobType.BROADCAST) {
                Assert.hasText(task.getWorker(), () -> "Broadcast job must be pre-assign worker: " + instance.getInstanceId());
                worker = Worker.deserialize(task.getWorker());
            }
            ExecuteTaskParam param = new ExecuteTaskParam(
                Operations.TRIGGER,
                task.getTaskId(),
                instance.getInstanceId(),
                job.getJobId(),
                jobType,
                instance.getTriggerTime(),
                worker
            );

            dispatchParams.add(new DispatchParam(param, job.getJobGroup(), routeStrategy));
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

    private boolean doDispatch(List<DispatchParam> dispatchParams) {
        Worker current = Worker.current();
        boolean result = true;
        for (DispatchParam dispatchParam : dispatchParams) {
            assignWorker(dispatchParam);

            ExecuteTaskParam param = dispatchParam.executeTaskParam();
            if (param.getWorker() == null) {
                // not found worker, delay retry
                retry(dispatchParam);
                result = false;
                continue;
            }
            try {
                boolean toLocal = timingWheel != null && current != null && current.equalsGroup(param.getWorker());
                boolean success = toLocal ? timingWheel.offer(param) : dispatch(param);
                if (!success) {
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
        ExecuteTaskParam param = dispatchParam.executeTaskParam();
        if (param.operation() != Operations.TRIGGER) {
            // if pause or cancel task operation, cannot assign worker
            Objects.requireNonNull(param.getWorker(), () -> param.operation() + " execution worker cannot be null: " + param.getTaskId());
            return;
        }

        if (param.getJobType() == JobType.BROADCAST) {
            Objects.requireNonNull(param.getWorker(), "Broadcast execution worker cannot be null: " + param.getTaskId());
            return;
        }

        List<Worker> workers = discoveryWorker.getDiscoveredServers(dispatchParam.group());
        if (CollectionUtils.isEmpty(workers)) {
            log.warn("Assign worker not found available worker");
            return;
        }

        ExecutionRouter executionRouter = ExecutionRouterRegistrar.get(dispatchParam.routeStrategy());
        Worker worker = executionRouter.route(dispatchParam.group(), param, workers);
        if (worker == null) {
            log.error("Assign worker to task failed: {} | {}", param.getInstanceId(), param.getTaskId());
        }
        param.setWorker(worker);
    }

    private void retry(DispatchParam dispatchParam) {
        if (dispatchParam.retried() >= maxRetryTimes) {
            // discard
            log.error("Dispatched task retried max times still failed: " + dispatchParam.executeTaskParam());
            return;
        }

        dispatchParam.retrying();
        asyncDelayedExecutor.put(DelayedData.of(dispatchParam, 1000L * IntMath.pow(dispatchParam.retried(), 2)));
    }

}

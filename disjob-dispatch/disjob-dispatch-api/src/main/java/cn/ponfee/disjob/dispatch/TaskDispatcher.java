/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.dispatch;

import cn.ponfee.disjob.common.base.Startable;
import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.common.concurrent.AsyncDelayedExecutor;
import cn.ponfee.disjob.common.concurrent.DelayedData;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.enums.Operations;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.core.param.ExecuteTaskParam;
import cn.ponfee.disjob.core.route.ExecutionRouter;
import cn.ponfee.disjob.core.route.ExecutionRouterRegistrar;
import cn.ponfee.disjob.registry.Discovery;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Supervisor dispatching task to worker.
 *
 * @author Ponfee
 */
public abstract class TaskDispatcher implements Startable {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final Discovery<Worker> discoveryWorker;
    private final TimingWheel<ExecuteTaskParam> timingWheel;

    private final int retryMaxCount;
    private final long retryBackoffPeriod;
    private final AsyncDelayedExecutor<DispatchParam> asyncDelayedExecutor;

    public TaskDispatcher(Discovery<Worker> discoveryWorker,
                          RetryProperties retryProperties,
                          @Nullable TimingWheel<ExecuteTaskParam> timingWheel) {
        Objects.requireNonNull(retryProperties, "Retry properties cannot be null.").check();
        this.discoveryWorker = discoveryWorker;
        this.timingWheel = timingWheel;

        this.retryMaxCount = retryProperties.getMaxCount();
        this.retryBackoffPeriod = retryProperties.getBackoffPeriod();
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
            .map(e -> new DispatchParam(e, null))
            .collect(Collectors.toList());
        return doDispatch(list);
    }

    /**
     * Assign a worker and dispatch to the assigned worker.
     *
     * @param param    the list of execution task param
     * @param jobGroup the job group
     * @return {@code true} if the first dispatch successful
     */
    public final boolean dispatch(List<ExecuteTaskParam> param, String jobGroup) {
        if (CollectionUtils.isEmpty(param)) {
            return false;
        }
        List<DispatchParam> list = param.stream()
            .peek(e -> Assert.isTrue(e.operation() == Operations.TRIGGER, "Dispatching execute param operation must be TRIGGER."))
            .map(e -> new DispatchParam(e, jobGroup))
            .collect(Collectors.toList());
        return doDispatch(list);
    }

    /**
     * Start do receive
     */
    @Override
    public void start() {
        // No-op
    }

    /**
     * Close resources if necessary.
     */
    @Override
    public void stop() {
        // No-op
    }

    private boolean doDispatch(List<DispatchParam> dispatchParams) {
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
                if (doDispatch(param)) {
                    log.info("Dispatched task {} | {} | {}", param.getTaskId(), param.getOperation(), param.getWorker());
                } else {
                    // dispatch failed, delay retry
                    retry(dispatchParam);
                    log.error("Dispatched task failed " + dispatchParam);
                    result = false;
                }
            } catch (Throwable t) {
                // dispatch error, delay retry
                retry(dispatchParam);
                log.error("Dispatch task error: " + dispatchParam, t);
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

        if (param.getRouteStrategy() == RouteStrategy.BROADCAST) {
            Objects.requireNonNull(param.getWorker(), () -> "Broadcast strategy worker cannot be null: " + param.getTaskId());
            return;
        }

        List<Worker> workers = discoveryWorker.getDiscoveredServers(dispatchParam.group());
        if (CollectionUtils.isEmpty(workers)) {
            log.warn("Assign worker not found available worker");
            return;
        }

        ExecutionRouter executionRouter = ExecutionRouterRegistrar.get(param.getRouteStrategy());
        Worker worker = executionRouter.route(dispatchParam.group(), param, workers);
        if (worker == null) {
            log.error("Assign worker to task failed: {} | {}", param.getInstanceId(), param.getTaskId());
        }
        param.setWorker(worker);
    }

    private boolean doDispatch(ExecuteTaskParam param) throws Exception {
        Worker current = Worker.current();
        if (timingWheel != null && current != null && current.equalsGroup(param.getWorker())) {
            // if the server both is supervisor & worker: dispatch to local worker
            return timingWheel.offer(param);
        } else {
            // dispatch to remote worker
            return dispatch(param);
        }
    }

    private void retry(DispatchParam dispatchParam) {
        if (dispatchParam.retried() >= retryMaxCount) {
            // discard
            log.error("Dispatched task retried max count still failed: " + dispatchParam.executeTaskParam());
            return;
        }

        dispatchParam.retrying();
        asyncDelayedExecutor.put(DelayedData.of(dispatchParam, retryBackoffPeriod * dispatchParam.retried()));
    }

}

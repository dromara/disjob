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
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.concurrent.AsyncDelayedExecutor;
import cn.ponfee.disjob.common.concurrent.DelayedData;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.dispatch.route.ExecutionRouterRegistrar;
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
    private final AsyncDelayedExecutor<DispatchTaskParam> asyncDelayedExecutor;

    protected TaskDispatcher(Discovery<Worker> discoveryWorker,
                             RetryProperties retryProperties,
                             @Nullable TimingWheel<ExecuteTaskParam> timingWheel) {
        Objects.requireNonNull(retryProperties, "Retry properties cannot be null.").check();
        this.discoveryWorker = discoveryWorker;
        this.timingWheel = timingWheel;

        this.retryMaxCount = retryProperties.getMaxCount();
        this.retryBackoffPeriod = retryProperties.getBackoffPeriod();
        this.asyncDelayedExecutor = new AsyncDelayedExecutor<>(5, e -> doDispatch(Collections.singletonList(e)));
    }

    /**
     * Dispatch the tasks to remote worker
     *
     * @param task the execution task param
     * @return {@code true} if dispatched successful
     * @throws Exception if dispatch occur error
     */
    protected abstract boolean dispatch(ExecuteTaskParam task) throws Exception;

    /**
     * Dispatch the task to specified worker, which the worker is executing this task
     * <p>this method is used to terminate(pause or cancel) the executing task
     *
     * @param tasks the list of execution task param
     * @return {@code true} if the first dispatch successful
     */
    public final boolean dispatch(List<ExecuteTaskParam> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return false;
        }
        List<DispatchTaskParam> params = tasks.stream()
            .peek(e -> {
                Assert.notNull(e.operation(), () -> "Dispatch task operation cannot be null: " + e);
                Assert.isTrue(e.operation().isNotTrigger(), () -> "Specific dispatch task operation cannot be trigger: " + e);
                Assert.notNull(e.getWorker(), () -> "Specific dispatch task worker cannot be null: " + e);
            })
            .map(e -> new DispatchTaskParam(e, null))
            .collect(Collectors.toList());
        return doDispatch(params);
    }

    /**
     * Assign a worker and dispatch to the assigned worker.
     *
     * @param tasks the list of execution task param
     * @param group the group
     * @return {@code true} if the first dispatch successful
     */
    public final boolean dispatch(List<ExecuteTaskParam> tasks, String group) {
        if (CollectionUtils.isEmpty(tasks)) {
            return false;
        }
        List<DispatchTaskParam> params = tasks.stream()
            .peek(e -> {
                Assert.notNull(e.operation(), () -> "Dispatch task operation cannot be null: " + e);
                Assert.isTrue(e.operation().isTrigger(), () -> "Assign dispatch task operation must be trigger: " + e);
                if (e.getRouteStrategy() == RouteStrategy.BROADCAST) {
                    Assert.notNull(e.getWorker(), () -> "Broadcast dispatch task worker cannot be null: " + e);
                }
            })
            .map(e -> new DispatchTaskParam(e, group))
            .collect(Collectors.toList());
        return doDispatch(params);
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

    // ------------------------------------------------------------private methods

    private boolean doDispatch(List<DispatchTaskParam> params) {
        params.stream()
            .filter(e -> e.executeTaskParam().operation().isTrigger())
            .filter(e -> e.executeTaskParam().getRouteStrategy() != RouteStrategy.BROADCAST)
            // setWorker(null): reset worker
            .peek(e -> e.executeTaskParam().setWorker(null))
            .collect(Collectors.groupingBy(e -> e.executeTaskParam().getInstanceId()))
            .forEach((instanceId, list) -> assignWorker(list));

        boolean result = true;
        for (DispatchTaskParam param : params) {
            ExecuteTaskParam task = param.executeTaskParam();
            if (task.getWorker() == null) {
                // if not found worker(assign worker failed), delay retry
                retry(param);
                result = false;
                continue;
            }

            try {
                doDispatch(task);
                log.info("Task trace [{}] dispatched: {} | {}", task.getTaskId(), task.getOperation(), task.getWorker());
            } catch (Throwable t) {
                // dispatch failed, delay retry
                retry(param);
                log.error("Dispatch task error: " + param, t);
                result = false;
            }
        }

        return result;
    }

    private void assignWorker(List<DispatchTaskParam> params) {
        DispatchTaskParam first = params.get(0);
        List<Worker> workers = discoveryWorker.getDiscoveredServers(first.group());
        if (CollectionUtils.isEmpty(workers)) {
            log.error("Not found available worker for assign to task.");
            return;
        }

        List<ExecuteTaskParam> tasks = Collects.convert(params, DispatchTaskParam::executeTaskParam);
        ExecutionRouterRegistrar.route(first.executeTaskParam().getRouteStrategy(), tasks, workers);
    }

    private void doDispatch(ExecuteTaskParam task) throws Exception {
        boolean result;
        if (timingWheel != null && task.getWorker().equals(Worker.current())) {
            // if the server both is supervisor & worker: dispatch to local worker
            log.info("Dispatching task to local worker {} | {} | {}", task.getTaskId(), task.getOperation(), task.getWorker());
            result = timingWheel.offer(task);
        } else {
            // dispatch to remote worker
            result = dispatch(task);
        }
        if (!result) {
            throw new TaskDispatchException("Dispatch task failed: " + task);
        }
    }

    private void retry(DispatchTaskParam param) {
        if (param.retried() >= retryMaxCount) {
            // discard
            log.error("Dispatched task retried max count still failed: {}", param.executeTaskParam());
            return;
        }

        param.retrying();
        asyncDelayedExecutor.put(DelayedData.of(param, retryBackoffPeriod * param.retried()));
    }

}

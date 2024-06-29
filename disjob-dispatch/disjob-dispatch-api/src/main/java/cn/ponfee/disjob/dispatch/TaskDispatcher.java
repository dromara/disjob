/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.dispatch;

import cn.ponfee.disjob.common.base.Startable;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.concurrent.AsyncDelayedExecutor;
import cn.ponfee.disjob.common.concurrent.DelayedData;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.dispatch.event.TaskDispatchFailedEvent;
import cn.ponfee.disjob.dispatch.route.ExecutionRouterRegistrar;
import cn.ponfee.disjob.registry.Discovery;
import com.google.common.math.IntMath;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
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

    private final ApplicationEventPublisher eventPublisher;
    private final Discovery<Worker> discoveryWorker;
    private final TaskReceiver taskReceiver;

    private final int retryMaxCount;
    private final long retryBackoffPeriod;
    private final AsyncDelayedExecutor<DispatchTaskParam> asyncDelayedExecutor;

    protected TaskDispatcher(ApplicationEventPublisher eventPublisher,
                             Discovery<Worker> discoveryWorker,
                             RetryProperties retryProperties,
                             @Nullable TaskReceiver taskReceiver) {
        Objects.requireNonNull(retryProperties, "Retry properties cannot be null.").check();
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
        this.discoveryWorker = Objects.requireNonNull(discoveryWorker);
        this.taskReceiver = taskReceiver;

        this.retryMaxCount = retryProperties.getMaxCount();
        this.retryBackoffPeriod = retryProperties.getBackoffPeriod();
        this.asyncDelayedExecutor = new AsyncDelayedExecutor<>(5, e -> dispatch0(Collections.singletonList(e)));
    }

    /**
     * Dispatch the tasks to remote worker
     *
     * @param task the execution task param
     * @return {@code true} if dispatched successful
     * @throws Exception if dispatch occur error
     */
    protected abstract boolean doDispatch(ExecuteTaskParam task) throws Exception;

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
        for (ExecuteTaskParam e : tasks) {
            Assert.notNull(e.getOperation(), () -> "Dispatch task operation cannot be null: " + e);
            Assert.isTrue(e.getOperation().isNotTrigger(), () -> "Specific dispatch task operation cannot be trigger: " + e);
            Assert.notNull(e.getWorker(), () -> "Specific dispatch task worker cannot be null: " + e);
        }
        return dispatch0(Collects.convert(tasks, e -> new DispatchTaskParam(e, null)));
    }

    /**
     * Assign a worker and dispatch to the assigned worker.
     *
     * @param group the group
     * @param tasks the list of execution task param
     * @return {@code true} if the first dispatch successful
     */
    public final boolean dispatch(String group, List<ExecuteTaskParam> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return false;
        }
        for (ExecuteTaskParam e : tasks) {
            Assert.notNull(e.getOperation(), () -> "Dispatch task operation cannot be null: " + e);
            Assert.isTrue(e.getOperation().isTrigger(), () -> "Assign dispatch task operation must be trigger: " + e);
            if (e.getRouteStrategy().isBroadcast()) {
                Assert.notNull(e.getWorker(), () -> "Broadcast dispatch task worker cannot be null: " + e);
            }
        }
        return dispatch0(Collects.convert(tasks, e -> new DispatchTaskParam(e, group)));
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
        asyncDelayedExecutor.doStop();
    }

    // ------------------------------------------------------------private methods

    private boolean dispatch0(List<DispatchTaskParam> params) {
        params.stream()
            .filter(e -> e.task().getWorker() == null)
            .collect(Collectors.groupingBy(e -> e.task().getInstanceId()))
            .forEach((instanceId, list) -> assignWorker(list));

        boolean result = true;
        for (DispatchTaskParam param : params) {
            ExecuteTaskParam task = param.task();
            log.info("Task trace [{}] dispatching: {}, {}, {}", task.getTaskId(), param.retried(), task.getOperation(), task.getWorker());
            try {
                doDispatch0(task);
                log.info("Task trace [{}] dispatched: {}, {}", task.getTaskId(), task.getOperation(), task.getWorker());
            } catch (Throwable t) {
                // dispatch failed, delay retry
                if (t instanceof TaskDispatchException) {
                    log.error("Dispatch task failed: {}, {}", t.getMessage(), param);
                } else {
                    log.error("Dispatch task error: " + param, t);
                }
                retry(param);
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

        List<ExecuteTaskParam> tasks = Collects.convert(params, DispatchTaskParam::task);
        ExecutionRouterRegistrar.route(first.task().getRouteStrategy(), tasks, workers);
    }

    private void doDispatch0(ExecuteTaskParam task) throws Exception {
        if (task.getWorker() == null) {
            throw new TaskDispatchException("unassigned");
        }

        boolean result;
        if (taskReceiver != null && task.getWorker().equals(Worker.current())) {
            // if current Supervisor also is a Worker role, then dispatch to this local worker
            log.info("Dispatching task to local worker {}, {}, {}", task.getTaskId(), task.getOperation(), task.getWorker());
            result = taskReceiver.receive(task);
        } else {
            // dispatch to remote worker
            result = doDispatch(task);
        }
        if (!result) {
            throw new TaskDispatchException("false");
        }
    }

    private void retry(DispatchTaskParam param) {
        ExecuteTaskParam task = param.task();
        if (param.retried() < retryMaxCount) {
            log.info("Delay retrying dispatch task [{}]: {}", param.retried(), task);
            int count = param.retrying();
            if (task.getRouteStrategy().isNotBroadcast() && task.getOperation().isTrigger()) {
                // clear assigned worker
                task.setWorker(null);
            }
            asyncDelayedExecutor.put(DelayedData.of(param, retryBackoffPeriod * IntMath.pow(count, 2)));
        } else {
            // discard
            log.error("Dispatched task retried max count still failed: {}", task);
            eventPublisher.publishEvent(new TaskDispatchFailedEvent(task.getJobId(), task.getInstanceId(), task.getTaskId()));
        }
    }

}

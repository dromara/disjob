/*
 * Copyright 2022-2026 Ponfee (http://www.ponfee.cn/)
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

package cn.ponfee.disjob.supervisor.dispatch;

import cn.ponfee.disjob.common.base.Startable;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.concurrent.AsyncDelayedExecutor;
import cn.ponfee.disjob.common.concurrent.DelayedData;
import cn.ponfee.disjob.common.spring.TransactionUtils;
import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.worker.Worker;
import cn.ponfee.disjob.core.worker.dto.ExecuteTaskParam;
import cn.ponfee.disjob.registry.Discovery;
import cn.ponfee.disjob.supervisor.component.WorkerClient;
import cn.ponfee.disjob.supervisor.dispatch.route.ExecutionRouterRegistrar;
import com.google.common.math.IntMath;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Supervisor dispatching task to worker.
 *
 * @author Ponfee
 */
@Component
public final class TaskDispatcher implements Startable {
    private static final Logger LOG = LoggerFactory.getLogger(TaskDispatcher.class);

    private final int retryMaxCount;
    private final long retryBackoffPeriod;
    private final ApplicationEventPublisher eventPublisher;
    private final Discovery<Worker> discoverWorker;
    private final WorkerClient workerClient;
    private final AsyncDelayedExecutor<DispatchTaskParam> asyncDelayedExecutor;

    public TaskDispatcher(RetryProperties retryConf,
                          ApplicationEventPublisher eventPublisher,
                          Discovery<Worker> discoverWorker,
                          WorkerClient workerClient) {
        Objects.requireNonNull(retryConf, "Retry properties cannot be null.").check();
        this.retryMaxCount = retryConf.getMaxCount();
        this.retryBackoffPeriod = retryConf.getBackoffPeriod();
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
        this.discoverWorker = Objects.requireNonNull(discoverWorker);
        this.workerClient = Objects.requireNonNull(workerClient);
        this.asyncDelayedExecutor = new AsyncDelayedExecutor<>(5, e -> dispatch0(Collections.singletonList(e)));
    }

    /**
     * Dispatch the task to specified worker, which the worker is executing this task
     * <p>this method is used to stop(pause or cancel) the executing task
     *
     * @param tasks the list of execution task param
     */
    public void dispatch(List<ExecuteTaskParam> tasks) {
        TransactionUtils.assertWithoutTransaction();
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }
        for (ExecuteTaskParam e : tasks) {
            Assert.notNull(e.getOperation(), () -> "Dispatch task operation cannot be null: " + e);
            Assert.isTrue(e.getOperation().isNotTrigger(), () -> "Specific dispatch task operation cannot be trigger: " + e);
            Assert.notNull(e.getWorker(), () -> "Specific dispatch task worker cannot be null: " + e);
        }
        dispatch0(Collects.convert(tasks, e -> new DispatchTaskParam(e, null)));
    }

    /**
     * Assign a worker and dispatch to the destination worker.
     *
     * @param group the group
     * @param tasks the list of execution task param
     */
    public void dispatch(String group, List<ExecuteTaskParam> tasks) {
        TransactionUtils.assertWithoutTransaction();
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }
        for (ExecuteTaskParam e : tasks) {
            Assert.notNull(e.getOperation(), () -> "Dispatch task operation cannot be null: " + e);
            Assert.isTrue(e.getOperation().isTrigger(), () -> "Assign dispatch task operation must be trigger: " + e);
            if (e.getRouteStrategy().isBroadcast()) {
                Assert.notNull(e.getWorker(), () -> "Broadcast dispatch task worker cannot be null: " + e);
            }
        }
        dispatch0(Collects.convert(tasks, e -> new DispatchTaskParam(e, group)));
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

    private void dispatch0(List<DispatchTaskParam> params) {
        params.stream()
            .filter(e -> e.task().getWorker() == null)
            .collect(Collectors.groupingBy(e -> e.task().getInstanceId()))
            .forEach((instanceId, list) -> assignWorker(list));

        for (DispatchTaskParam param : params) {
            ExecuteTaskParam task = param.task();
            LOG.info("Task trace [{}] dispatching: {}, {}, {}", task.getTaskId(), task.getOperation(), task.getWorker(), param.retried());
            try {
                doDispatch(task);
                LOG.info("Task trace [{}] dispatched: {}, {}", task.getTaskId(), task.getOperation(), task.getWorker());
            } catch (Throwable t) {
                // dispatch failed, delay retry
                if (t instanceof TaskDispatchException) {
                    LOG.error("Dispatch task failed: {}, {}", t.getMessage(), param);
                } else {
                    LOG.error("Dispatch task error: " + param, t);
                }
                retry(param);
            }
        }
    }

    private void assignWorker(List<DispatchTaskParam> params) {
        DispatchTaskParam first = params.get(0);
        String group = first.group();
        List<Worker> workers = discoverWorker.getAliveServers(group);
        if (CollectionUtils.isEmpty(workers)) {
            LOG.error("Not found available [{}] worker for assign task.", group);
            return;
        }

        List<ExecuteTaskParam> tasks = Collects.convert(params, DispatchTaskParam::task);
        ExecutionRouterRegistrar.route(first.task().getRouteStrategy(), tasks, workers);
    }

    private void doDispatch(ExecuteTaskParam task) throws Exception {
        Worker worker = task.getWorker();
        if (worker == null) {
            throw new TaskDispatchException("unassigned");
        }
        // dispatch the task to destination worker
        if (!workerClient.destination(worker).receiveTask(task)) {
            throw new TaskDispatchException("false");
        }
    }

    private void retry(DispatchTaskParam param) {
        ExecuteTaskParam task = param.task();
        if (param.retried() < retryMaxCount) {
            LOG.info("Delay retrying dispatch task [{}]: {}", param.retried(), task);
            int count = param.retrying();
            if (task.getRouteStrategy().isNotBroadcast() && task.getOperation().isTrigger()) {
                // clear assigned worker
                task.setWorker(null);
            }
            asyncDelayedExecutor.put(DelayedData.of(param, retryBackoffPeriod * IntMath.pow(count, 2)));
        } else {
            // discard
            LOG.error("Dispatched task retried max count still failed: {}", task);
            eventPublisher.publishEvent(TaskDispatchFailedEvent.of(task));
        }
    }

}

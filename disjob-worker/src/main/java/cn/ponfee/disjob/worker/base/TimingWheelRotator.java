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

package cn.ponfee.disjob.worker.base;

import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.base.Startable;
import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.concurrent.LoopThread;
import cn.ponfee.disjob.common.concurrent.NamedThreadFactory;
import cn.ponfee.disjob.common.concurrent.PeriodExecutor;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.exception.Throwables;
import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.supervisor.Supervisor;
import cn.ponfee.disjob.core.supervisor.SupervisorRpcService;
import cn.ponfee.disjob.core.worker.dto.ExecuteTaskParam;
import cn.ponfee.disjob.registry.Discovery;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * The thread for rotating timing wheel.
 *
 * @author Ponfee
 */
@Slf4j
public class TimingWheelRotator extends SingletonClassConstraint implements Startable {

    private final SupervisorRpcService supervisorRpcClient;
    private final Discovery<Supervisor> discoverSupervisor;
    private final TimingWheel<ExecuteTaskParam> timingWheel;
    private final WorkerThreadPool workerThreadPool;
    private final LoopThread heartbeatThread;
    private final ExecutorService processExecutor;
    private final PeriodExecutor logPrinter = new PeriodExecutor(30000, () -> log.warn("Not found available supervisor."));

    public TimingWheelRotator(SupervisorRpcService supervisorRpcClient,
                              Discovery<Supervisor> discoverSupervisor,
                              TimingWheel<ExecuteTaskParam> timingWheel,
                              WorkerThreadPool threadPool,
                              int processThreadPoolSize) {
        this.supervisorRpcClient = supervisorRpcClient;
        this.discoverSupervisor = discoverSupervisor;
        this.timingWheel = timingWheel;
        this.workerThreadPool = threadPool;

        this.heartbeatThread = new LoopThread("timing_wheel_rotate", timingWheel.getTickMs(), 0, this::rotate);

        int actualProcessPoolSize = Math.max(1, processThreadPoolSize);
        this.processExecutor = ThreadPoolExecutors.builder()
            .corePoolSize(actualProcessPoolSize)
            .maximumPoolSize(actualProcessPoolSize)
            .workQueue(new LinkedBlockingQueue<>(Integer.MAX_VALUE))
            .keepAliveTimeSeconds(300)
            .rejectedHandler(ThreadPoolExecutors.CALLER_RUNS)
            .threadFactory(NamedThreadFactory.builder().prefix("timing_wheel_process").daemon(true).uncaughtExceptionHandler(log).build())
            .build();
    }

    @Override
    public void start() {
        heartbeatThread.start();
    }

    @Override
    public void stop() {
        if (heartbeatThread.terminate()) {
            ThreadPoolExecutors.shutdown(processExecutor, 2);
        }
    }

    private void rotate() {
        // check has available supervisors
        if (!discoverSupervisor.hasAliveServer()) {
            logPrinter.execute();
            return;
        }

        final List<ExecuteTaskParam> tasks = timingWheel.poll();
        if (CollectionUtils.isNotEmpty(tasks)) {
            processExecutor.execute(() -> {
                updateTasks(tasks);
                submitTasks(tasks);
            });
        }
    }

    private void updateTasks(List<ExecuteTaskParam> tasks) {
        List<Long> taskIds = tasks.stream()
            // 筛选：触发器任务 && 非广播任务(广播任务的worker不可修改) && 不存在于线程池中
            .filter(e -> e.getOperation().isTrigger() && e.getRouteStrategy().isNotBroadcast() && isNotExists(e))
            .map(ExecuteTaskParam::getTaskId)
            .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(taskIds)) {
            String worker = Collects.getFirst(tasks).getWorker().serialize();
            for (List<Long> ids : Lists.partition(taskIds, JobConstants.PROCESS_BATCH_SIZE)) {
                try {
                    supervisorRpcClient.updateTaskWorker(ids, worker);
                } catch (Throwable t) {
                    log.error("Update task worker error: {}", ids, t);
                    Throwables.rethrowIfFatal(t);
                }
            }
        }
    }

    private void submitTasks(List<ExecuteTaskParam> tasks) {
        // 筛选：非触发器任务(暂停、恢复、取消) || 不存在于线程池中的任务
        tasks.stream().filter(e -> e.getOperation().isNotTrigger() || isNotExists(e)).forEach(e -> {
            String triggerTime = Dates.DATETIME_MILLI_FORMAT.format(e.getTriggerTime());
            boolean res = workerThreadPool.submit(new WorkerTask(e));
            log.info("Task trace [{}] triggered: {}, {}, {}, {}", e.getTaskId(), e.getOperation(), e.getWorker(), triggerTime, res);
        });
    }

    private boolean isNotExists(ExecuteTaskParam task) {
        return !workerThreadPool.existsTask(task.getTaskId());
    }

}

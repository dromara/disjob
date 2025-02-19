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

package cn.ponfee.disjob.worker.base;

import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.base.Startable;
import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.concurrent.LoopThread;
import cn.ponfee.disjob.common.concurrent.NamedThreadFactory;
import cn.ponfee.disjob.common.concurrent.PeriodExecutor;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.SupervisorRpcService;
import cn.ponfee.disjob.dispatch.ExecuteTaskParam;
import cn.ponfee.disjob.registry.Discovery;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import static cn.ponfee.disjob.core.base.JobConstants.PROCESS_BATCH_SIZE;

/**
 * The thread for rotating timing wheel.
 *
 * @author Ponfee
 */
public class TimingWheelRotator extends SingletonClassConstraint implements Startable {

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Logger LOG = LoggerFactory.getLogger(TimingWheelRotator.class);

    private final SupervisorRpcService supervisorRpcClient;
    private final Discovery<Supervisor> discoverSupervisor;
    private final TimingWheel<ExecuteTaskParam> timingWheel;
    private final WorkerThreadPool workerThreadPool;
    private final LoopThread heartbeatThread;
    private final ExecutorService processExecutor;
    private final PeriodExecutor logPrinter = new PeriodExecutor(30000, () -> LOG.warn("Not found available supervisor."));

    public TimingWheelRotator(SupervisorRpcService supervisorRpcClient,
                              Discovery<Supervisor> discoverSupervisor,
                              TimingWheel<ExecuteTaskParam> timingWheel,
                              WorkerThreadPool threadPool,
                              int processThreadPoolSize) {
        this.supervisorRpcClient = supervisorRpcClient;
        this.discoverSupervisor = discoverSupervisor;
        this.timingWheel = timingWheel;
        this.workerThreadPool = threadPool;

        this.heartbeatThread = new LoopThread("timing_wheel_rotate", timingWheel.getTickMs(), 0, this::process);

        int actualProcessPoolSize = Math.max(1, processThreadPoolSize);
        this.processExecutor = ThreadPoolExecutors.builder()
            .corePoolSize(actualProcessPoolSize)
            .maximumPoolSize(actualProcessPoolSize)
            .workQueue(new LinkedBlockingQueue<>(Integer.MAX_VALUE))
            .keepAliveTimeSeconds(300)
            .rejectedHandler(ThreadPoolExecutors.CALLER_RUNS)
            .threadFactory(NamedThreadFactory.builder().prefix("timing_wheel_process").uncaughtExceptionHandler(LOG).build())
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

    private void process() {
        // check has available supervisors
        if (!discoverSupervisor.hasDiscoveredServers()) {
            logPrinter.execute();
            return;
        }

        final List<ExecuteTaskParam> tasks = timingWheel.poll();
        if (CollectionUtils.isNotEmpty(tasks)) {
            processExecutor.execute(() -> process(tasks));
        }
    }

    private void process(List<ExecuteTaskParam> tasks) {
        updateTaskWorker(tasks);
        for (ExecuteTaskParam e : tasks) {
            String triggerTime = DATE_FORMAT.format(e.getTriggerTime());
            LOG.info("Task trace [{}] triggered: {}, {}, {}", e.getTaskId(), e.getOperation(), e.getWorker(), triggerTime);
            workerThreadPool.submit(new WorkerTask(e));
        }
    }

    private void updateTaskWorker(List<ExecuteTaskParam> list) {
        List<Long> taskIds = list.stream()
            // 广播任务分派的worker不可修改，需要剔除
            .filter(e -> e.getRouteStrategy().isNotBroadcast())
            .map(ExecuteTaskParam::getTaskId)
            .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(taskIds)) {
            return;
        }

        String worker = Collects.getFirst(list).getWorker().serialize();
        for (List<Long> ids : Lists.partition(taskIds, PROCESS_BATCH_SIZE)) {
            ThrowingRunnable.doCaught(() -> supervisorRpcClient.updateTaskWorker(worker, ids), () -> "Update task worker error: " + ids);
        }
    }

}
